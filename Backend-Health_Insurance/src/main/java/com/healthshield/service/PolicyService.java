package com.healthshield.service;

import com.healthshield.dto.request.PolicyMemberRequest;
import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.PolicyRenewalRequest;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.Gender;
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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyMemberRepository policyMemberRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final PremiumQuoteRepository premiumQuoteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService; // ← NEW
    private final EmailService emailService;               // ← NEW
    private final AuditLogService auditLogService;         // ← NEW

    @Value("${file.upload.dir:uploads/policies}")
    private String uploadDir;

    @Transactional
    public PolicyResponse purchasePolicy(Long userId, PolicyPurchaseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        InsurancePlan plan = insurancePlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + request.getPlanId()));

        if (!plan.getIsActive()) {
            throw new BadRequestException("Insurance plan is not active");
        }

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
                        .relationship(Relationship.valueOf(memberReq.getRelationship().toUpperCase()))
                        .dateOfBirth(memberReq.getDateOfBirth())
                        .gender(memberReq.getGender() != null ? Gender.valueOf(memberReq.getGender().toUpperCase()) : null)
                        .preExistingDiseases(memberReq.getPreExistingDiseases())
                        .build();
                policyMemberRepository.save(member);
            }
        }

        savedPolicy = policyRepository.findById(savedPolicy.getPolicyId()).orElse(savedPolicy);

        // ── NEW: Notify customer on application submitted ──
        notificationService.sendNotification(
                user.getEmail(),
                "Your policy application " + policyNumber + " for " + plan.getPlanName()
                        + " has been submitted and is under review.",
                NotificationType.GENERAL
        );

        // ── NEW: Audit log ──
        auditLogService.log("POLICY_APPLIED", "CUSTOMER", user.getEmail(),
                "Policy " + policyNumber + " applied for plan: " + plan.getPlanName());
        // Notify ADMIN when new policy comes in
        userRepository.findAll().stream()
                .filter(u -> u.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
                .forEach(admin -> notificationService.sendNotification(
                        admin.getEmail(),
                        "New policy application " + policyNumber + " from "
                                + user.getFirstName() + " " + user.getLastName(),
                        NotificationType.GENERAL
                ));

        return mapToResponse(savedPolicy);
    }

    @Transactional
    public PolicyResponse purchasePolicyWithDocument(Long userId, PolicyPurchaseRequest request,
                                                     MultipartFile healthCheckReport) {
        PolicyResponse response = purchasePolicy(userId, request);
        if (healthCheckReport != null && !healthCheckReport.isEmpty()) {
            String policyUploadDir = uploadDir + "/" + response.getPolicyNumber();
            Path uploadPath = Paths.get(policyUploadDir);
            try {
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String fileName = org.springframework.util.StringUtils.cleanPath(
                        healthCheckReport.getOriginalFilename());
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(healthCheckReport.getInputStream(), filePath,
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("Health check report uploaded for policy {}: {}",
                        response.getPolicyNumber(), fileName);
            } catch (IOException e) {
                log.error("Failed to upload health check report: {}", e.getMessage());
            }
        }
        return response;
    }

    public List<PolicyResponse> getPoliciesByUser(Long userId) {
        return policyRepository.findByUserUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PolicyResponse getPolicyById(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (!isAuthorized(currentUser, policy)) {
            throw new UnauthorizedException("You are not authorized to view this policy");
        }
        return mapToResponse(policy);
    }

    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PolicyResponse cancelPolicy(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!isAuthorized(currentUser, policy)) {
            throw new UnauthorizedException("You are not authorized to cancel this policy");
        }
        if (policy.getPolicyStatus() == PolicyStatus.CANCELLED) {
            throw new BadRequestException("Policy is already cancelled");
        }

        policy.setPolicyStatus(PolicyStatus.CANCELLED);
        Policy saved = policyRepository.save(policy);

        // ── NEW: Notify customer ──
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "Your policy " + policy.getPolicyNumber() + " has been cancelled.",
                NotificationType.POLICY_REJECTED
        );
        emailService.sendStatusChangeEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(), "CANCELLED"
        );
        auditLogService.log("POLICY_CANCELLED", currentUser.getAuthorities()
                        .iterator().next().getAuthority().replace("ROLE_", ""),
                currentUser.getEmail(), "Policy " + policy.getPolicyNumber() + " cancelled");

        return mapToResponse(saved);
    }

    public List<PolicyMemberResponse> getPolicyMembers(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (!isAuthorized(currentUser, policy)) {
            throw new UnauthorizedException("You are not authorized to view this policy's members");
        }
        return policyMemberRepository.findByPolicyPolicyId(policyId).stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void activatePolicy(Policy policy) {
        policy.setPolicyStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(LocalDate.now());
        policy.setEndDate(LocalDate.now().plusMonths(policy.getPlan().getPlanDurationMonths()));
        policy.setRemainingCoverage(policy.getCoverageAmount());
        policy.setTotalClaimedAmount(BigDecimal.ZERO);
        policyRepository.save(policy);

        // ── NEW: Greeting email to customer ──
        emailService.sendPolicyActivationEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(),
                policy.getPlan().getPlanName(),
                policy.getPremiumAmount(),
                policy.getCoverageAmount(),
                policy.getStartDate(),
                policy.getEndDate()
        );

        // ── NEW: In-app notification ──
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "🎉 Congratulations! Your policy " + policy.getPolicyNumber()
                        + " is now ACTIVE. Coverage: ₹" + policy.getCoverageAmount(),
                NotificationType.POLICY_APPROVED
        );

        auditLogService.log("POLICY_ACTIVATED", "SYSTEM",
                policy.getUser().getEmail(),
                "Policy " + policy.getPolicyNumber() + " activated");

        log.info("Policy {} activated | Start: {} | End: {}",
                policy.getPolicyNumber(), policy.getStartDate(), policy.getEndDate());
    }

    @Transactional
    public void expirePolicyForTesting(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        policy.setPolicyStatus(PolicyStatus.EXPIRED);
        policyRepository.save(policy);
    }

    @Transactional
    public PolicyResponse renewPolicy(User currentUser, Long policyId, PolicyRenewalRequest request) {
        Policy originalPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!originalPolicy.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You can only renew your own policies");
        }
        if (originalPolicy.getPolicyStatus() != PolicyStatus.EXPIRED
                && originalPolicy.getPolicyStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Only EXPIRED or ACTIVE policies can be renewed.");
        }

        InsurancePlan plan = originalPolicy.getPlan();
        if (!plan.getIsActive()) {
            throw new BadRequestException("The plan '" + plan.getPlanName()
                    + "' has been discontinued. Please choose a new plan.");
        }

        BigDecimal noClaimBonus = BigDecimal.ZERO;
        BigDecimal totalClaimed = originalPolicy.getTotalClaimedAmount() != null
                ? originalPolicy.getTotalClaimedAmount() : BigDecimal.ZERO;

        if (totalClaimed.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal previousNCB = originalPolicy.getNoClaimBonus() != null
                    ? originalPolicy.getNoClaimBonus() : BigDecimal.ZERO;
            noClaimBonus = previousNCB.add(BigDecimal.valueOf(5)).min(BigDecimal.valueOf(25));
        }

        BigDecimal basePremium = originalPolicy.getPremiumAmount();
        BigDecimal renewalPremium = basePremium.multiply(BigDecimal.valueOf(1.03));
        if (noClaimBonus.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ncbDiscount = renewalPremium
                    .multiply(noClaimBonus)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            renewalPremium = renewalPremium.subtract(ncbDiscount);
        }
        renewalPremium = renewalPremium.setScale(2, RoundingMode.HALF_UP);

        String newPolicyNumber = generatePolicyNumber();
        int newRenewalCount = (originalPolicy.getRenewalCount() != null
                ? originalPolicy.getRenewalCount() : 0) + 1;

        Policy renewedPolicy = Policy.builder()
                .policyNumber(newPolicyNumber)
                .user(currentUser)
                .plan(plan)
                .assignedUnderwriter(originalPolicy.getAssignedUnderwriter())
                .premiumAmount(renewalPremium)
                .coverageAmount(plan.getCoverageAmount())
                .remainingCoverage(plan.getCoverageAmount())
                .totalClaimedAmount(BigDecimal.ZERO)
                .policyStatus(PolicyStatus.PENDING)
                .nomineeName(request.getNomineeName() != null
                        ? request.getNomineeName() : originalPolicy.getNomineeName())
                .nomineeRelationship(request.getNomineeRelationship() != null
                        ? request.getNomineeRelationship() : originalPolicy.getNomineeRelationship())
                .renewalCount(newRenewalCount)
                .originalPolicy(originalPolicy)
                .noClaimBonus(noClaimBonus)
                .members(new ArrayList<>())
                .claims(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        Policy saved = policyRepository.save(renewedPolicy);

        if (originalPolicy.getMembers() != null) {
            for (PolicyMember origMember : originalPolicy.getMembers()) {
                PolicyMember newMember = PolicyMember.builder()
                        .policy(saved)
                        .memberName(origMember.getMemberName())
                        .relationship(origMember.getRelationship())
                        .dateOfBirth(origMember.getDateOfBirth())
                        .gender(origMember.getGender())
                        .preExistingDiseases(origMember.getPreExistingDiseases())
                        .build();
                policyMemberRepository.save(newMember);
            }
        }

        originalPolicy.setPolicyStatus(PolicyStatus.RENEWED);
        policyRepository.save(originalPolicy);
        saved = policyRepository.findById(saved.getPolicyId()).orElse(saved);

        // ── NEW: Notify on renewal ──
        notificationService.sendNotification(
                currentUser.getEmail(),
                "Your policy renewal " + newPolicyNumber + " has been submitted."
                        + (noClaimBonus.compareTo(BigDecimal.ZERO) > 0
                        ? " No-Claim Bonus of " + noClaimBonus + "% applied!" : ""),
                NotificationType.POLICY_RENEWED
        );
        auditLogService.log("POLICY_RENEWED", "CUSTOMER", currentUser.getEmail(),
                "Policy " + originalPolicy.getPolicyNumber() + " renewed to " + newPolicyNumber);

        log.info("Policy {} renewed to {} | NCB: {}% | Premium: ₹{}",
                originalPolicy.getPolicyNumber(), newPolicyNumber,
                noClaimBonus, renewalPremium);

        return mapToResponse(saved);
    }

    @Transactional
    public PolicyResponse reapplyPolicy(Long userId, Long policyId,
                                        PolicyPurchaseRequest request,
                                        MultipartFile healthCheckReport) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!policy.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only reapply for your own policies");
        }
        if (policy.getPolicyStatus() != PolicyStatus.CONCERN_RAISED) {
            throw new BadRequestException("Only CONCERN_RAISED policies can be reapplied.");
        }

        if (request.getNomineeName() != null && !request.getNomineeName().isBlank())
            policy.setNomineeName(request.getNomineeName());
        if (request.getNomineeRelationship() != null && !request.getNomineeRelationship().isBlank())
            policy.setNomineeRelationship(request.getNomineeRelationship());

        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            policyMemberRepository.deleteByPolicyPolicyId(policyId);
            policyMemberRepository.flush();
            for (PolicyMemberRequest memberReq : request.getMembers()) {
                PolicyMember member = PolicyMember.builder()
                        .policy(policy)
                        .memberName(memberReq.getMemberName())
                        .relationship(Relationship.valueOf(memberReq.getRelationship().toUpperCase()))
                        .dateOfBirth(memberReq.getDateOfBirth())
                        .gender(memberReq.getGender() != null
                                ? Gender.valueOf(memberReq.getGender().toUpperCase()) : null)
                        .preExistingDiseases(memberReq.getPreExistingDiseases())
                        .build();
                policyMemberRepository.save(member);
            }
        }

        if (healthCheckReport != null && !healthCheckReport.isEmpty()) {
            String policyUploadDir = uploadDir + "/" + policy.getPolicyNumber();
            Path uploadPath = Paths.get(policyUploadDir);
            try {
                if (Files.exists(uploadPath)) {
                    Files.list(uploadPath).filter(Files::isRegularFile).forEach(f -> {
                        try { Files.delete(f); } catch (IOException ignored) {}
                    });
                } else {
                    Files.createDirectories(uploadPath);
                }
                String fileName = org.springframework.util.StringUtils.cleanPath(
                        healthCheckReport.getOriginalFilename());
                Files.copy(healthCheckReport.getInputStream(), uploadPath.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Failed to upload health check report: {}", e.getMessage());
            }
        }

        policy.setPolicyStatus(PolicyStatus.PENDING);
        policy.setUnderwriterRemarks(null);
        policy.setAssignedUnderwriter(null);
        policy.setAssignedAt(null);
        policy.setQuoteAmount(null);
        policy.setPremiumAmount(null);
        policy.setCommissionAmount(null);

        Policy saved = policyRepository.save(policy);
        saved = policyRepository.findById(saved.getPolicyId()).orElse(saved);

        // ── NEW: Notify on reapply ──
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "Your policy " + policy.getPolicyNumber()
                        + " has been resubmitted and is under review.",
                NotificationType.GENERAL
        );
        auditLogService.log("POLICY_REAPPLIED", "CUSTOMER", policy.getUser().getEmail(),
                "Policy " + policy.getPolicyNumber() + " reapplied after concern raised");

        return mapToResponse(saved);
    }

    // ── existing private methods unchanged ──

    private BigDecimal calculatePremium(User user, InsurancePlan plan,
                                        PolicyPurchaseRequest request) {
        if (request.getQuoteId() != null) {
            PremiumQuote quote = premiumQuoteRepository.findById(request.getQuoteId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Premium quote not found with id: " + request.getQuoteId()));
            if (quote.getUser() != null && !quote.getUser().getUserId().equals(user.getUserId()))
                throw new BadRequestException("This quote does not belong to you");
            if (!quote.getPlan().getPlanId().equals(plan.getPlanId()))
                throw new BadRequestException("Quote is for a different plan.");
            return quote.getCalculatedPremium();
        }

        int age = 30;
        if (user instanceof Customer customer && customer.getDateOfBirth() != null)
            age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();

        double ageFactor = age <= 30 ? 1.0 : age <= 40 ? 1.2 : age <= 50 ? 1.5
                : age <= 60 ? 1.8 : 2.2;
        int members = (request.getMembers() != null) ? request.getMembers().size() + 1 : 1;
        double memberFactor = 1.0 + (members - 1) * 0.7;

        return plan.getBasePremiumAmount()
                .multiply(BigDecimal.valueOf(ageFactor))
                .multiply(BigDecimal.valueOf(memberFactor))
                .setScale(2, RoundingMode.HALF_UP);
    }

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
        return isAdmin || isUnderwriter
                || policy.getUser().getUserId().equals(user.getUserId());
    }

    private PolicyResponse mapToResponse(Policy policy) {
        List<PolicyMemberResponse> memberResponses = new ArrayList<>();
        if (policy.getMembers() != null) {
            memberResponses = policy.getMembers().stream()
                    .map(this::mapMemberToResponse)
                    .collect(Collectors.toList());
        }

        PolicyResponse.PolicyResponseBuilder builder = PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .policyNumber(policy.getPolicyNumber())
                .customerId(policy.getUser().getUserId())
                .customerName(policy.getUser().getFirstName()
                        + " " + policy.getUser().getLastName())
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
                .assignedAt(policy.getAssignedAt());

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
                .relationship(member.getRelationship() != null
                        ? member.getRelationship().name() : null)
                .dateOfBirth(member.getDateOfBirth())
                .gender(member.getGender() != null ? member.getGender().name() : null)
                .preExistingDiseases(member.getPreExistingDiseases())
                .build();
    }

    public String getPolicyDocumentPath(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with id: " + policyId));
        String policyDir = uploadDir + "/" + policy.getPolicyNumber();
        Path dirPath = Paths.get(policyDir);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try {
                return Files.list(dirPath).filter(Files::isRegularFile).findFirst()
                        .map(Path::toString).orElse(null);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}