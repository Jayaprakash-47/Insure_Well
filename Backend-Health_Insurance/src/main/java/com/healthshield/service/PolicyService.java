package com.healthshield.service;

import com.healthshield.dto.request.PolicyMemberRequest;
import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.PolicyRenewalRequest;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.Gender;
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

        // Premium is NOT calculated here — underwriter will calculate and send the quote
        Policy policy = Policy.builder()
                .policyNumber(policyNumber)
                .user(user)
                .plan(plan)
                .premiumAmount(null)          // Set by underwriter when sending quote
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

        return mapToResponse(savedPolicy);
    }

    @Transactional
    public PolicyResponse purchasePolicyWithDocument(Long userId, PolicyPurchaseRequest request, MultipartFile healthCheckReport) {
        PolicyResponse response = purchasePolicy(userId, request);

        if (healthCheckReport != null && !healthCheckReport.isEmpty()) {
            String policyUploadDir = uploadDir + "/" + response.getPolicyNumber();
            Path uploadPath = Paths.get(policyUploadDir);
            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = org.springframework.util.StringUtils.cleanPath(healthCheckReport.getOriginalFilename());
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(healthCheckReport.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                log.info("Health check report uploaded for policy {}: {}", response.getPolicyNumber(), fileName);
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

        log.info("Policy {} activated | Start: {} | End: {}",
                policy.getPolicyNumber(), policy.getStartDate(), policy.getEndDate());
    }

    /**
     * For testing purposes, immediately expires a policy.
     */
    @Transactional
    public void expirePolicyForTesting(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        policy.setPolicyStatus(PolicyStatus.EXPIRED);
        policyRepository.save(policy);
    }

    // =================== POLICY RENEWAL ===================

    /**
     * Renew an expired or expiring policy.
     * Creates a new policy linked to the original with No-Claim Bonus if applicable.
     */
    @Transactional
    public PolicyResponse renewPolicy(User currentUser, Long policyId, PolicyRenewalRequest request) {
        Policy originalPolicy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        if (!originalPolicy.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You can only renew your own policies");
        }

        // Only expired or active policies can be renewed
        if (originalPolicy.getPolicyStatus() != PolicyStatus.EXPIRED
                && originalPolicy.getPolicyStatus() != PolicyStatus.ACTIVE) {
            throw new BadRequestException("Only EXPIRED or ACTIVE policies can be renewed. Current status: "
                    + originalPolicy.getPolicyStatus());
        }

        InsurancePlan plan = originalPolicy.getPlan();
        if (!plan.getIsActive()) {
            throw new BadRequestException("The insurance plan '" + plan.getPlanName()
                    + "' has been discontinued. Please choose a new plan.");
        }

        // Calculate No-Claim Bonus
        BigDecimal noClaimBonus = BigDecimal.ZERO;
        BigDecimal totalClaimed = originalPolicy.getTotalClaimedAmount() != null
                ? originalPolicy.getTotalClaimedAmount() : BigDecimal.ZERO;

        if (totalClaimed.compareTo(BigDecimal.ZERO) == 0) {
            // No claims filed — reward with increasing NCB
            int renewalCount = originalPolicy.getRenewalCount() != null ? originalPolicy.getRenewalCount() : 0;
            BigDecimal previousNCB = originalPolicy.getNoClaimBonus() != null
                    ? originalPolicy.getNoClaimBonus() : BigDecimal.ZERO;

            // NCB increases: 5% → 10% → 15% → 20% → 25% (max)
            noClaimBonus = previousNCB.add(BigDecimal.valueOf(5))
                    .min(BigDecimal.valueOf(25));

            log.info("No-Claim Bonus applied: {}% for policy {}", noClaimBonus, originalPolicy.getPolicyNumber());
        }

        // Calculate renewal premium (may increase with age, decreased by NCB)
        BigDecimal basePremium = originalPolicy.getPremiumAmount();

        // Age increase factor (1 year older)
        BigDecimal agingFactor = BigDecimal.valueOf(1.03); // 3% increase per year
        BigDecimal renewalPremium = basePremium.multiply(agingFactor);

        // Apply NCB discount
        if (noClaimBonus.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ncbDiscount = renewalPremium.multiply(noClaimBonus)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            renewalPremium = renewalPremium.subtract(ncbDiscount);
        }

        renewalPremium = renewalPremium.setScale(2, RoundingMode.HALF_UP);

        String newPolicyNumber = generatePolicyNumber();
        int newRenewalCount = (originalPolicy.getRenewalCount() != null ? originalPolicy.getRenewalCount() : 0) + 1;

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
                .nomineeName(request.getNomineeName() != null ? request.getNomineeName() : originalPolicy.getNomineeName())
                .nomineeRelationship(request.getNomineeRelationship() != null ? request.getNomineeRelationship() : originalPolicy.getNomineeRelationship())
                .renewalCount(newRenewalCount)
                .originalPolicy(originalPolicy)
                .noClaimBonus(noClaimBonus)
                .members(new ArrayList<>())
                .claims(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        Policy saved = policyRepository.save(renewedPolicy);

        // Copy existing members to renewed policy
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

        // Mark original as renewed
        originalPolicy.setPolicyStatus(PolicyStatus.RENEWED);
        policyRepository.save(originalPolicy);

        saved = policyRepository.findById(saved.getPolicyId()).orElse(saved);

        log.info("Policy {} renewed to {} | NCB: {}% | Premium: ₹{} → ₹{}",
                originalPolicy.getPolicyNumber(), newPolicyNumber,
                noClaimBonus, originalPolicy.getPremiumAmount(), renewalPremium);

        return mapToResponse(saved);
    }

    // =================== PREMIUM CALCULATION ===================

    private BigDecimal calculatePremium(User user, InsurancePlan plan, PolicyPurchaseRequest request) {
        if (request.getQuoteId() != null) {
            PremiumQuote quote = premiumQuoteRepository.findById(request.getQuoteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Premium quote not found with id: " + request.getQuoteId()));

            if (quote.getUser() != null && !quote.getUser().getUserId().equals(user.getUserId())) {
                throw new BadRequestException("This quote does not belong to you");
            }

            if (!quote.getPlan().getPlanId().equals(plan.getPlanId())) {
                throw new BadRequestException("Quote is for a different plan. Quote plan: "
                        + quote.getPlan().getPlanName() + ", Selected plan: " + plan.getPlanName());
            }

            return quote.getCalculatedPremium();
        }

        int age = 30;
        if (user instanceof Customer customer) {
            if (customer.getDateOfBirth() != null) {
                age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
            }
        }

        double ageFactor;
        if (age <= 30) ageFactor = 1.0;
        else if (age <= 40) ageFactor = 1.2;
        else if (age <= 50) ageFactor = 1.5;
        else if (age <= 60) ageFactor = 1.8;
        else ageFactor = 2.2;

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
            number = "HHS-" + Year.now().getValue() + "-" + String.format("%06d", random.nextInt(999999));
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
                    .map(this::mapMemberToResponse)
                    .collect(Collectors.toList());
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
                .assignedAt(policy.getAssignedAt());

        if (policy.getAssignedUnderwriter() != null) {
            builder.underwriterId(policy.getAssignedUnderwriter().getUserId())
                    .underwriterName(policy.getAssignedUnderwriter().getFirstName()
                            + " " + policy.getAssignedUnderwriter().getLastName());
        }

        if (policy.getOriginalPolicy() != null) {
            builder.originalPolicyId(policy.getOriginalPolicy().getPolicyId());
        }

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

    /** Get the path to the health check report document for a policy */
    public String getPolicyDocumentPath(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        String policyDir = uploadDir + "/" + policy.getPolicyNumber();
        Path dirPath = Paths.get(policyDir);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            try {
                return Files.list(dirPath)
                        .filter(Files::isRegularFile)
                        .findFirst()
                        .map(Path::toString)
                        .orElse(null);
            } catch (IOException e) {
                log.error("Error reading policy document directory: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }
}
