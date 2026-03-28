package com.healthshield.service;

import com.healthshield.dto.request.PolicyMemberRequest;
import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.PolicyRenewalRequest;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.Gender;
import com.healthshield.enums.KycStatus;
import com.healthshield.enums.NotificationType;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.enums.Relationship;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.Period;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository            policyRepository;
    private final PolicyMemberRepository      policyMemberRepository;
    private final InsurancePlanRepository     insurancePlanRepository;
    private final PremiumQuoteRepository      premiumQuoteRepository;
    private final UserRepository              userRepository;
    private final NotificationService         notificationService;
    private final EmailService                emailService;
    private final AuditLogService             auditLogService;
    private final HealthReportAnalysisService healthReportAnalysisService;

    @Value("${file.upload.dir:uploads/policies}")
    private String uploadDir;

    private static final String HEALTH_REPORT_SUBFOLDER = "health-report";
    private static final String AADHAAR_SUBFOLDER       = "aadhaar";

    // =================== PURCHASE POLICY ===================

    @Transactional
    public PolicyResponse purchasePolicy(Long userId, PolicyPurchaseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        InsurancePlan plan = insurancePlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Insurance plan not found with id: " + request.getPlanId()));

        if (!plan.getIsActive())
            throw new BadRequestException("Insurance plan is not active");

        // ── ENFORCE KYC-FIRST POLICY ── (COMMENTED OUT FOR TESTING)
        // if (user instanceof com.healthshield.entity.Customer customer) {
        //     if (!Boolean.TRUE.equals(customer.getAadhaarVerified()) && (request.getKycTransactionId() == null || request.getKycTransactionId().isBlank())) {
        //         throw new BadRequestException("KYC Verification required. Please complete eAadhaar verification during application.");
        //     }
        //     if (request.getKycTransactionId() != null && !request.getKycTransactionId().isBlank()) {
        //         customer.setAadhaarVerified(true);
        //         userRepository.save(customer);
        //     }
        // }

        String policyNumber = generatePolicyNumber();

        Policy policy = Policy.builder()
                .policyNumber(policyNumber)
                .user(user)
                .plan(plan)
                .premiumAmount(null)
                .coverageAmount(plan.getCoverageAmount())
                .remainingCoverage(plan.getCoverageAmount())
                .totalClaimedAmount(BigDecimal.ZERO)
                .policyStatus(PolicyStatus.PENDING)
                .nomineeName(request.getNomineeName())
                .nomineeRelationship(request.getNomineeRelationship())
                .renewalCount(0)
                .noClaimBonus(BigDecimal.ZERO)
                .kycStatus((request.getKycTransactionId() != null && !request.getKycTransactionId().isBlank()) ? KycStatus.VERIFIED : KycStatus.PENDING)
                .aiAnalysisDone(false)
                .members(new ArrayList<>())
                .claims(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        Policy savedPolicy = policyRepository.save(policy);

        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (PolicyMemberRequest memberReq : request.getMembers()) {
                PolicyMember member = PolicyMember.builder()
                        .policy(savedPolicy)
                        .memberName(memberReq.getMemberName())
                        .relationship(Relationship.valueOf(
                                memberReq.getRelationship().toUpperCase()))
                        .dateOfBirth(memberReq.getDateOfBirth())
                        .gender(memberReq.getGender() != null
                                ? Gender.valueOf(memberReq.getGender().toUpperCase()) : null)
                        .preExistingDiseases(memberReq.getPreExistingDiseases())
                        .build();
                policyMemberRepository.save(member);
            }
        }

        savedPolicy = policyRepository.findById(savedPolicy.getPolicyId()).orElse(savedPolicy);

        notificationService.sendNotification(
                user.getEmail(),
                "Your policy application " + policyNumber + " for "
                        + plan.getPlanName() + " has been submitted and is under review.",
                NotificationType.GENERAL);

        auditLogService.log("POLICY_APPLIED", "CUSTOMER", user.getEmail(),
                "Policy " + policyNumber + " applied for plan: " + plan.getPlanName());

        userRepository.findAll().stream()
                .filter(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .forEach(admin -> notificationService.sendNotification(
                        admin.getEmail(),
                        "New policy application " + policyNumber + " from "
                                + user.getFirstName() + " " + user.getLastName(),
                        NotificationType.GENERAL));

        return mapToResponse(savedPolicy);
    }

    // =================== PURCHASE WITH DOCUMENTS ===================

    @Transactional
    public PolicyResponse purchasePolicyWithDocument(Long userId,
                                                     PolicyPurchaseRequest request,
                                                     MultipartFile healthCheckReport,
                                                     MultipartFile aadhaarDocument) {
        PolicyResponse response = purchasePolicy(userId, request);

        Policy policy = policyRepository.findByPolicyNumber(response.getPolicyNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found: " + response.getPolicyNumber()));

        String healthReportPath = null;

        if (healthCheckReport != null && !healthCheckReport.isEmpty()) {
            healthReportPath = saveFile(
                    healthCheckReport, response.getPolicyNumber(), HEALTH_REPORT_SUBFOLDER);
        }

        if (aadhaarDocument != null && !aadhaarDocument.isEmpty()) {
            String aadhaarPath = saveFile(
                    aadhaarDocument, response.getPolicyNumber(), AADHAAR_SUBFOLDER);
            if (aadhaarPath != null) policy.setAadhaarDocumentPath(aadhaarPath);
        }

        policyRepository.save(policy);

        if (healthReportPath != null) {
            final Long   policyId  = policy.getPolicyId();
            final String finalPath = healthReportPath;
            healthReportAnalysisService.analyzeAndUpdatePolicy(finalPath, policyId, this);
        }

        return mapToResponse(policyRepository.findById(policy.getPolicyId()).orElse(policy));
    }

    // =================== AI: UPDATE EXTRACTED CONDITIONS ===================

    @Transactional
    public void updateExtractedConditions(Long policyId, String conditions) {
        policyRepository.findById(policyId).ifPresent(policy -> {
            policy.setExtractedConditions(conditions);
            policy.setAiAnalysisDone(true);
            policyRepository.save(policy);

            log.info("AI conditions saved for policy {}: {}", policyId, conditions);

            if (policy.getAssignedUnderwriter() != null) {
                String underwriterEmail = policy.getAssignedUnderwriter().getEmail();
                boolean hasConditions = conditions != null
                        && !conditions.isBlank()
                        && !conditions.equalsIgnoreCase("None");
                String msg = "🤖 AI analysis complete for policy "
                        + policy.getPolicyNumber() + ". "
                        + (hasConditions
                        ? "Conditions found: " + conditions
                        : "No conditions detected — clean report.");
                notificationService.sendNotification(underwriterEmail, msg, NotificationType.GENERAL);
            }
        });
    }

    // =================== UNDERWRITER: CALCULATE QUOTE ===================

    @Transactional
    public BigDecimal calculateUnderwriterQuote(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        BigDecimal base = policy.getPlan().getBasePremiumAmount();

        int maxAge = 30;
        if (policy.getMembers() != null && !policy.getMembers().isEmpty()) {
            maxAge = policy.getMembers().stream()
                    .filter(m -> m.getDateOfBirth() != null)
                    .mapToInt(m -> Period.between(m.getDateOfBirth(), LocalDate.now()).getYears())
                    .max().orElse(30);
        }

        double ageFactor;
        if      (maxAge <= 25) ageFactor = 1.0;
        else if (maxAge <= 30) ageFactor = 1.1;
        else if (maxAge <= 40) ageFactor = 1.3;
        else if (maxAge <= 50) ageFactor = 1.6;
        else if (maxAge <= 60) ageFactor = 2.0;
        else                   ageFactor = 2.5;

        int memberCount = (policy.getMembers() != null) ? policy.getMembers().size() : 1;
        double memberFactor = 1.0 + (memberCount - 1) * 0.15;

        double diseaseFactor = 1.0;
        String conditions = policy.getExtractedConditions();

        if (conditions != null && !conditions.isBlank() && !conditions.equalsIgnoreCase("None")) {
            List<String> condList = Arrays.stream(conditions.split(","))
                    .map(String::trim).map(String::toLowerCase)
                    .filter(c -> !c.isBlank()).toList();

            for (String c : condList) {
                if      (c.contains("cancer")   || c.contains("tumor"))               diseaseFactor += 0.50;
                else if (c.contains("heart")    || c.contains("cardiac")
                        || c.contains("coronary"))                                     diseaseFactor += 0.40;
                else if (c.contains("kidney")   || c.contains("renal")
                        || c.contains("liver")  || c.contains("cirrhosis"))            diseaseFactor += 0.35;
                else if (c.contains("stroke")   || c.contains("epilepsy"))             diseaseFactor += 0.30;
                else if (c.contains("type 2 diabetes") || c.contains("type 1 diabetes")
                        || c.contains("diabetic") || c.contains("diabetes"))           diseaseFactor += 0.25;
                else if (c.contains("hypertension") || c.contains("blood pressure"))  diseaseFactor += 0.20;
                else if (c.contains("cholesterol") || c.contains("hyperlipidemia")
                        || c.contains("obesity") || c.contains("overweight"))         diseaseFactor += 0.12;
                else if (c.contains("asthma")   || c.contains("copd")
                        || c.contains("thyroid") || c.contains("anemia"))             diseaseFactor += 0.08;
                else                                                                   diseaseFactor += 0.05;
            }
            diseaseFactor = Math.min(diseaseFactor, 3.0);
        }

        BigDecimal finalQuote = base
                .multiply(BigDecimal.valueOf(ageFactor))
                .multiply(BigDecimal.valueOf(memberFactor))
                .multiply(BigDecimal.valueOf(diseaseFactor))
                .setScale(2, RoundingMode.HALF_UP);

        policy.setQuoteAmount(finalQuote);
        policyRepository.save(policy);
        return finalQuote;
    }

    // =================== KYC ===================

    @Transactional
    public PolicyResponse verifyKyc(Long underwriterId, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        validateUnderwriterOwnership(underwriterId, policy);
        policy.setKycStatus(KycStatus.VERIFIED);
        Policy saved = policyRepository.save(policy);

        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "✅ Your KYC has been verified for policy " + policy.getPolicyNumber()
                        + ". The underwriter will now send your premium quote.",
                NotificationType.GENERAL);

        auditLogService.log("KYC_VERIFIED", "UNDERWRITER",
                policy.getAssignedUnderwriter().getEmail(),
                "KYC verified for policy " + policy.getPolicyNumber());

        return mapToResponse(saved);
    }

    // ── FIX: rejectKyc now sets CONCERN_RAISED → customer sees Reapply button ──
    @Transactional
    public PolicyResponse rejectKyc(Long underwriterId, Long policyId, String reason) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));

        validateUnderwriterOwnership(underwriterId, policy);

        // Mark KYC as rejected
        policy.setKycStatus(KycStatus.REJECTED);

        // FIX: Set CONCERN_RAISED so the customer sees the Reapply button in My Policies
        policy.setPolicyStatus(PolicyStatus.CONCERN_RAISED);

        // FIX: Store reason as underwriterRemarks so it appears in the concern box
        policy.setUnderwriterRemarks("KYC Rejected: " + reason);

        Policy saved = policyRepository.save(policy);

        // FIX: Clear, actionable notification with next steps
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "❌ Your Aadhaar verification was rejected for policy "
                        + policy.getPolicyNumber() + ". Reason: " + reason
                        + ". Please go to My Policies → click 'Reapply' and upload a clear Aadhaar document.",
                NotificationType.GENERAL);

        // Also send email
        try {
            emailService.sendStatusChangeEmail(
                    policy.getUser().getEmail(),
                    policy.getUser().getFirstName(),
                    policy.getPolicyNumber(),
                    "KYC_REJECTED");
        } catch (Exception e) {
            log.warn("KYC rejection email failed: {}", e.getMessage());
        }

        auditLogService.log("KYC_REJECTED", "UNDERWRITER",
                policy.getAssignedUnderwriter().getEmail(),
                "KYC rejected for policy " + policy.getPolicyNumber() + ". Reason: " + reason);

        return mapToResponse(saved);
    }

    private void validateUnderwriterOwnership(Long underwriterId, Policy policy) {
        if (policy.getAssignedUnderwriter() == null
                || !policy.getAssignedUnderwriter().getUserId().equals(underwriterId))
            throw new UnauthorizedException("This policy is not assigned to you");
    }

    // =================== CRUD ===================

    public List<PolicyResponse> getPoliciesByUser(Long userId) {
        return policyRepository.findByUserUserId(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public PolicyResponse getPolicyById(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (!isAuthorized(currentUser, policy))
            throw new UnauthorizedException("You are not authorized to view this policy");
        return mapToResponse(policy);
    }

    public Policy findById(Long policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
    }

    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public PolicyResponse cancelPolicy(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (!isAuthorized(currentUser, policy))
            throw new UnauthorizedException("You are not authorized to cancel this policy");
        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED)
            throw new BadRequestException("Policy is already cancelled");

        policy.setPolicyStatus(PolicyStatus.CANCELLED);
        Policy saved = policyRepository.save(policy);

        notificationService.sendNotification(policy.getUser().getEmail(),
                "Your policy " + policy.getPolicyNumber() + " has been cancelled.",
                NotificationType.POLICY_REJECTED);
        emailService.sendStatusChangeEmail(policy.getUser().getEmail(),
                policy.getUser().getFirstName(), policy.getPolicyNumber(), "CANCELLED");
        auditLogService.log("POLICY_CANCELLED",
                currentUser.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                currentUser.getEmail(), "Policy " + policy.getPolicyNumber() + " cancelled");

        return mapToResponse(saved);
    }

    public List<PolicyMemberResponse> getPolicyMembers(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (!isAuthorized(currentUser, policy))
            throw new UnauthorizedException("You are not authorized to view this policy's members");
        return policyMemberRepository.findByPolicyPolicyId(policyId).stream()
                .map(this::mapMemberToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void activatePolicy(Policy policy) {
        policy.setPolicyStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(LocalDate.now());
        policy.setEndDate(LocalDate.now().plusMonths(policy.getPlan().getPlanDurationMonths()));
        policy.setRemainingCoverage(policy.getCoverageAmount());
        policy.setTotalClaimedAmount(BigDecimal.ZERO);
        policyRepository.save(policy);

        emailService.sendPolicyActivationEmail(
                policy.getUser().getEmail(), policy.getUser().getFirstName(),
                policy.getPolicyNumber(), policy.getPlan().getPlanName(),
                policy.getPremiumAmount(), policy.getCoverageAmount(),
                policy.getStartDate(), policy.getEndDate());

        notificationService.sendNotification(policy.getUser().getEmail(),
                "🎉 Congratulations! Your policy " + policy.getPolicyNumber()
                        + " is now ACTIVE. Coverage: ₹" + policy.getCoverageAmount(),
                NotificationType.POLICY_APPROVED);

        auditLogService.log("POLICY_ACTIVATED", "SYSTEM", policy.getUser().getEmail(),
                "Policy " + policy.getPolicyNumber() + " activated");
    }

    // =================== RENEWAL ===================

    @Transactional
    public PolicyResponse renewPolicy(User currentUser, Long policyId, PolicyRenewalRequest request) {
        Policy originalPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!originalPolicy.getUser().getUserId().equals(currentUser.getUserId()))
            throw new UnauthorizedException("You can only renew your own policies");
        if (originalPolicy.getPolicyStatus() != PolicyStatus.EXPIRED
                && originalPolicy.getPolicyStatus() != PolicyStatus.ACTIVE)
            throw new BadRequestException("Only EXPIRED or ACTIVE policies can be renewed.");

        InsurancePlan plan = originalPolicy.getPlan();
        if (!plan.getIsActive())
            throw new BadRequestException("The plan '" + plan.getPlanName()
                    + "' has been discontinued. Please choose a new plan.");

        BigDecimal noClaimBonus = BigDecimal.ZERO;
        BigDecimal totalClaimed = originalPolicy.getTotalClaimedAmount() != null
                ? originalPolicy.getTotalClaimedAmount() : BigDecimal.ZERO;

        if (totalClaimed.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal previousNCB = originalPolicy.getNoClaimBonus() != null
                    ? originalPolicy.getNoClaimBonus() : BigDecimal.ZERO;
            noClaimBonus = previousNCB.add(BigDecimal.valueOf(5)).min(BigDecimal.valueOf(25));
        }

        BigDecimal renewalPremium = originalPolicy.getPremiumAmount().multiply(BigDecimal.valueOf(1.03));
        if (noClaimBonus.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ncbDiscount = renewalPremium
                    .multiply(noClaimBonus).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            renewalPremium = renewalPremium.subtract(ncbDiscount);
        }
        renewalPremium = renewalPremium.setScale(2, RoundingMode.HALF_UP);

        String newPolicyNumber = generatePolicyNumber();
        int newRenewalCount = (originalPolicy.getRenewalCount() != null
                ? originalPolicy.getRenewalCount() : 0) + 1;

        Policy renewedPolicy = Policy.builder()
                .policyNumber(newPolicyNumber).user(currentUser).plan(plan)
                .assignedUnderwriter(originalPolicy.getAssignedUnderwriter())
                .premiumAmount(renewalPremium).coverageAmount(plan.getCoverageAmount())
                .remainingCoverage(plan.getCoverageAmount()).totalClaimedAmount(BigDecimal.ZERO)
                .policyStatus(PolicyStatus.PENDING)
                .nomineeName(request.getNomineeName() != null
                        ? request.getNomineeName() : originalPolicy.getNomineeName())
                .nomineeRelationship(request.getNomineeRelationship() != null
                        ? request.getNomineeRelationship() : originalPolicy.getNomineeRelationship())
                .renewalCount(newRenewalCount).originalPolicy(originalPolicy)
                .noClaimBonus(noClaimBonus)
                .aadhaarDocumentPath(originalPolicy.getAadhaarDocumentPath())
                .kycStatus(KycStatus.PENDING).aiAnalysisDone(false)
                .members(new ArrayList<>()).claims(new ArrayList<>()).payments(new ArrayList<>())
                .build();

        Policy saved = policyRepository.save(renewedPolicy);

        if (originalPolicy.getMembers() != null) {
            for (PolicyMember m : originalPolicy.getMembers()) {
                policyMemberRepository.save(PolicyMember.builder()
                        .policy(saved).memberName(m.getMemberName())
                        .relationship(m.getRelationship()).dateOfBirth(m.getDateOfBirth())
                        .gender(m.getGender()).preExistingDiseases(m.getPreExistingDiseases())
                        .build());
            }
        }

        originalPolicy.setPolicyStatus(PolicyStatus.RENEWED);
        policyRepository.save(originalPolicy);
        saved = policyRepository.findById(saved.getPolicyId()).orElse(saved);

        notificationService.sendNotification(currentUser.getEmail(),
                "Your policy renewal " + newPolicyNumber + " has been submitted."
                        + (noClaimBonus.compareTo(BigDecimal.ZERO) > 0
                        ? " No-Claim Bonus of " + noClaimBonus + "% applied!" : ""),
                NotificationType.POLICY_RENEWED);
        auditLogService.log("POLICY_RENEWED", "CUSTOMER", currentUser.getEmail(),
                "Policy " + originalPolicy.getPolicyNumber() + " renewed to " + newPolicyNumber);

        return mapToResponse(saved);
    }

    // =================== REAPPLY ===================
    // Handles both CONCERN_RAISED (underwriter concern) and KYC_REJECTED (now also CONCERN_RAISED)

    @Transactional
    public PolicyResponse reapplyPolicy(Long userId, Long policyId,
                                        PolicyPurchaseRequest request,
                                        MultipartFile healthCheckReport,
                                        MultipartFile aadhaarDocument) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!policy.getUser().getUserId().equals(userId))
            throw new UnauthorizedException("You can only reapply for your own policies");

        if (policy.getPolicyStatus() != PolicyStatus.CONCERN_RAISED)
            throw new BadRequestException("Only CONCERN_RAISED policies can be reapplied.");

        if (request.getNomineeName() != null && !request.getNomineeName().isBlank())
            policy.setNomineeName(request.getNomineeName());
        if (request.getNomineeRelationship() != null && !request.getNomineeRelationship().isBlank())
            policy.setNomineeRelationship(request.getNomineeRelationship());

        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            policyMemberRepository.deleteByPolicyPolicyId(policyId);
            policyMemberRepository.flush();
            for (PolicyMemberRequest memberReq : request.getMembers()) {
                policyMemberRepository.save(PolicyMember.builder()
                        .policy(policy)
                        .memberName(memberReq.getMemberName())
                        .relationship(Relationship.valueOf(memberReq.getRelationship().toUpperCase()))
                        .dateOfBirth(memberReq.getDateOfBirth())
                        .gender(memberReq.getGender() != null
                                ? Gender.valueOf(memberReq.getGender().toUpperCase()) : null)
                        .preExistingDiseases(memberReq.getPreExistingDiseases())
                        .build());
            }
        }

        String newHealthPath = null;
        if (healthCheckReport != null && !healthCheckReport.isEmpty()) {
            clearSubfolder(policy.getPolicyNumber(), HEALTH_REPORT_SUBFOLDER);
            newHealthPath = saveFile(healthCheckReport, policy.getPolicyNumber(), HEALTH_REPORT_SUBFOLDER);
        }

        // FIX: Handle Aadhaar re-upload — resets KYC to PENDING for re-verification
        if (aadhaarDocument != null && !aadhaarDocument.isEmpty()) {
            clearSubfolder(policy.getPolicyNumber(), AADHAAR_SUBFOLDER);
            String aadhaarPath = saveFile(aadhaarDocument, policy.getPolicyNumber(), AADHAAR_SUBFOLDER);
            if (aadhaarPath != null) {
                policy.setAadhaarDocumentPath(aadhaarPath);
                policy.setKycStatus(KycStatus.PENDING);
                log.info("Aadhaar re-uploaded for policy {} — KYC reset to PENDING",
                        policy.getPolicyNumber());
            }
        }

        policy.setPolicyStatus(PolicyStatus.PENDING);
        policy.setUnderwriterRemarks(null);
        policy.setAssignedUnderwriter(null);
        policy.setAssignedAt(null);
        policy.setQuoteAmount(null);
        policy.setPremiumAmount(null);
        policy.setCommissionAmount(null);
        policy.setAiAnalysisDone(false);
        policy.setExtractedConditions(null);

        Policy saved = policyRepository.save(policy);
        saved = policyRepository.findById(saved.getPolicyId()).orElse(saved);

        if (newHealthPath != null) {
            final Long   pid = saved.getPolicyId();
            final String fp  = newHealthPath;
            healthReportAnalysisService.analyzeAndUpdatePolicy(fp, pid, this);
        }

        notificationService.sendNotification(policy.getUser().getEmail(),
                "✅ Your policy " + policy.getPolicyNumber()
                        + " has been resubmitted with updated documents and is under review.",
                NotificationType.GENERAL);

        auditLogService.log("POLICY_REAPPLIED", "CUSTOMER", policy.getUser().getEmail(),
                "Policy " + policy.getPolicyNumber() + " reapplied after concern/KYC rejection");

        return mapToResponse(saved);
    }

    // =================== DOCUMENT PATHS ===================

    public String getPolicyDocumentPath(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        String policyDir = uploadDir + "/" + policy.getPolicyNumber() + "/" + HEALTH_REPORT_SUBFOLDER;
        Path dirPath = Paths.get(policyDir);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try {
                return Files.list(dirPath).filter(Files::isRegularFile)
                        .findFirst().map(Path::toString).orElse(null);
            } catch (IOException e) { return null; }
        }
        return null;
    }

    public String getAadhaarDocumentPath(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        return policy.getAadhaarDocumentPath();
    }

    @Transactional
    public void expirePolicyForTesting(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        policy.setPolicyStatus(PolicyStatus.EXPIRED);
        policyRepository.save(policy);
    }

    // =================== FILE HELPERS ===================

    private String saveFile(MultipartFile file, String policyNumber, String subfolder) {
        String dirPath    = uploadDir + "/" + policyNumber + "/" + subfolder;
        Path   uploadPath = Paths.get(dirPath);
        try {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[{}] {} saved: {}", policyNumber, subfolder, fileName);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save {} for policy {}: {}", subfolder, policyNumber, e.getMessage());
            return null;
        }
    }

    private void clearSubfolder(String policyNumber, String subfolder) {
        Path dirPath = Paths.get(uploadDir + "/" + policyNumber + "/" + subfolder);
        if (Files.exists(dirPath)) {
            try {
                Files.list(dirPath).filter(Files::isRegularFile).forEach(f -> {
                    try { Files.delete(f); } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
    }

    // =================== PRIVATE HELPERS ===================

    private String generatePolicyNumber() {
        String number;
        Random random = new Random();
        do {
            number = "HHS-" + Year.now().getValue() + "-"
                    + String.format("%06d", random.nextInt(999999));
        } while (policyRepository.existsByPolicyNumber(number));
        return number;
    }

    private boolean isAuthorized(User user, Policy policy) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isUnderwriter = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_UNDERWRITER"));
        return isAdmin || isUnderwriter || policy.getUser().getUserId().equals(user.getUserId());
    }

    private PolicyResponse mapToResponse(Policy policy) {
        List<PolicyMemberResponse> memberResponses = new ArrayList<>();
        if (policy.getMembers() != null) {
            memberResponses = policy.getMembers().stream()
                    .map(this::mapMemberToResponse).collect(Collectors.toList());
        }

        PolicyResponse.PolicyResponseBuilder builder = PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getUser().getUserId())
                .customerName(policy.getUser().getFirstName() + " " + policy.getUser().getLastName())
                .planId(policy.getPlan().getPlanId())
                .planName(policy.getPlan().getPlanName())
                .premiumAmount(policy.getPremiumAmount())
                .coverageAmount(policy.getCoverageAmount())
                .remainingCoverage(policy.getRemainingCoverage())
                .totalClaimedAmount(policy.getTotalClaimedAmount())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .policyStatus(policy.getPolicyStatus().name())
                .nomineeName(policy.getNomineeName())
                .nomineeRelationship(policy.getNomineeRelationship())
                .createdAt(policy.getCreatedAt())
                .members(memberResponses)
                .commissionAmount(policy.getCommissionAmount())
                .renewalCount(policy.getRenewalCount())
                .noClaimBonus(policy.getNoClaimBonus())
                .quoteAmount(policy.getQuoteAmount())
                .waitingPeriodMonths(policy.getPlan().getWaitingPeriodMonths())
                .assignedAt(policy.getAssignedAt())
                .extractedConditions(policy.getExtractedConditions())
                .aiAnalysisDone(policy.getAiAnalysisDone())
                .kycStatus(policy.getKycStatus() != null ? policy.getKycStatus().name() : "PENDING");

        if (policy.getAssignedUnderwriter() != null)
            builder.underwriterId(policy.getAssignedUnderwriter().getUserId())
                    .underwriterName(policy.getAssignedUnderwriter().getFirstName()
                            + " " + policy.getAssignedUnderwriter().getLastName());

        if (policy.getOriginalPolicy() != null)
            builder.originalPolicyId(policy.getOriginalPolicy().getPolicyId());

        builder.underwriterRemarks(policy.getUnderwriterRemarks());
        return builder.build();
    }

    private PolicyMemberResponse mapMemberToResponse(PolicyMember member) {
        return PolicyMemberResponse.builder()
                .memberId(member.getMemberId())
                .memberName(member.getMemberName())
                .relationship(member.getRelationship() != null ? member.getRelationship().name() : null)
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender() != null ? member.getGender().name() : null)
                .preExistingDiseases(member.getPreExistingDiseases())
                .build();
    }
}