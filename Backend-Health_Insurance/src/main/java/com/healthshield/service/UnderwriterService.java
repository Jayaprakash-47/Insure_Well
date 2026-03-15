package com.healthshield.service;

import com.healthshield.dto.request.UnderwriterQuoteRequest;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.UnderwriterDashboardResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.NotificationType;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnderwriterService {

    private final UnderwriterRepository underwriterRepository;
    private final PolicyRepository policyRepository;
    private final PolicyMemberRepository policyMemberRepository;
    private final InsurancePlanService insurancePlanService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;


    // =================== DASHBOARD ===================

    public UnderwriterDashboardResponse getDashboard(Long underwriterId) {
        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new ResourceNotFoundException("Underwriter not found with id: " + underwriterId));

        List<Policy> myPolicies = policyRepository.findByAssignedUnderwriterUserId(underwriterId);

        long pendingAssignments = myPolicies.stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ASSIGNED)
                .count();

        BigDecimal totalCommission = myPolicies.stream()
                .filter(p -> p.getCommissionAmount() != null)
                .map(Policy::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long customersServed = myPolicies.stream()
                .map(p -> p.getUser().getUserId())
                .distinct()
                .count();

        long activePolicies = myPolicies.stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ACTIVE)
                .count();

        BigDecimal totalPremium = myPolicies.stream()
                .map(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UnderwriterDashboardResponse.builder()
                .underwriterId(underwriter.getUserId())
                .underwriterName(underwriter.getFirstName() + " " + underwriter.getLastName())
                .email(underwriter.getEmail())
                .licenseNumber(underwriter.getLicenseNumber())
                .specialization(underwriter.getSpecialization())
                .commissionPercentage(underwriter.getCommissionPercentage())
                .totalQuotesSent(underwriter.getTotalQuotesSent())
                .totalCommissionEarned(totalCommission)
                .totalCustomersServed(customersServed)
                .activePolicies(activePolicies)
                .totalPremiumGenerated(totalPremium)
                .pendingAssignments(pendingAssignments)
                .build();
    }

    // =================== PENDING ASSIGNMENTS ===================

    public List<PolicyResponse> getPendingAssignments(Long underwriterId) {
        return policyRepository.findByAssignedUnderwriterUserId(underwriterId).stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ASSIGNED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getMyPolicies(Long underwriterId) {
        return policyRepository.findByAssignedUnderwriterUserId(underwriterId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =================== SEND QUOTE ===================

    @Transactional
    public PolicyResponse sendPremiumQuote(Long underwriterId, Long policyId, UnderwriterQuoteRequest request) {
        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new ResourceNotFoundException("Underwriter not found with id: " + underwriterId));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        // Validate this policy is assigned to this underwriter
        if (policy.getAssignedUnderwriter() == null
                || !policy.getAssignedUnderwriter().getUserId().equals(underwriterId)) {
            throw new UnauthorizedException("This policy application is not assigned to you");
        }

        if (policy.getPolicyStatus() != PolicyStatus.ASSIGNED) {
            throw new BadRequestException("Only ASSIGNED policies can receive a quote. Current status: " + policy.getPolicyStatus());
        }

        // Calculate commission
        BigDecimal commissionAmount = request.getQuoteAmount()
                .multiply(underwriter.getCommissionPercentage() != null ? underwriter.getCommissionPercentage() : BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        policy.setQuoteAmount(request.getQuoteAmount());
        policy.setPremiumAmount(request.getQuoteAmount());
        policy.setCommissionAmount(commissionAmount);
        policy.setPolicyStatus(PolicyStatus.QUOTE_SENT);

        // Update underwriter's total quotes count
        underwriter.setTotalQuotesSent(underwriter.getTotalQuotesSent() + 1);
        underwriterRepository.save(underwriter);

        Policy saved = policyRepository.save(policy);

        log.info("Underwriter {} sent quote ₹{} for policy {}", underwriter.getLicenseNumber(),
                request.getQuoteAmount(), policy.getPolicyNumber());
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "📨 Your premium quote for policy " + policy.getPolicyNumber()
                        + " is ₹" + request.getQuoteAmount()
                        + ". Please log in to review and make payment.",
                NotificationType.QUOTE_RECEIVED
        );
        emailService.sendStatusChangeEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(), "QUOTE_SENT"
        );
        auditLogService.log("QUOTE_SENT", "UNDERWRITER", underwriter.getEmail(),
                "Quote ₹" + request.getQuoteAmount()
                        + " sent for policy " + policy.getPolicyNumber());
        return mapToResponse(saved);
    }

    /** Auto-calculate the quote based on risk factors */
    @Transactional(readOnly = true)
    public BigDecimal calculateQuoteForPolicy(Long underwriterId, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found: " + policyId));

        if (policy.getAssignedUnderwriter() == null
                || !policy.getAssignedUnderwriter().getUserId().equals(underwriterId)) {
            throw new UnauthorizedException("This policy is not assigned to you");
        }

        int maxAge = 0;
        double diseaseFactor = 1.0;
        int membersCount = policy.getMembers() != null
                ? policy.getMembers().size() : 1;

        if (policy.getMembers() != null) {
            for (PolicyMember m : policy.getMembers()) {

                // ── Age factor ──
                if (m.getDateOfBirth() != null) {
                    int age = java.time.Period.between(
                            m.getDateOfBirth(),
                            java.time.LocalDate.now()).getYears();
                    if (age > maxAge) maxAge = age;
                }

                // ── NEW: Disease-based risk recognition ──
                if (m.getPreExistingDiseases() != null
                        && !m.getPreExistingDiseases().trim().isEmpty()
                        && !m.getPreExistingDiseases().equalsIgnoreCase("none")) {

                    double memberDiseaseFactor =
                            calculateDiseaseFactor(m.getPreExistingDiseases());

                    // Take the highest disease factor across all members
                    if (memberDiseaseFactor > diseaseFactor) {
                        diseaseFactor = memberDiseaseFactor;
                    }
                }
            }
        }

        if (maxAge == 0) maxAge = 30;

        double ageFactor;
        if (maxAge <= 30)      ageFactor = 1.0;
        else if (maxAge <= 40) ageFactor = 1.2;
        else if (maxAge <= 50) ageFactor = 1.5;
        else if (maxAge <= 60) ageFactor = 1.8;
        else                   ageFactor = 2.2;

        double memberFactor = 1.0 + (membersCount - 1) * 0.7;

        return policy.getPlan().getBasePremiumAmount()
                .multiply(BigDecimal.valueOf(ageFactor))
                .multiply(BigDecimal.valueOf(diseaseFactor))
                .multiply(BigDecimal.valueOf(memberFactor))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // ── NEW: Disease risk factor calculator ──
    private double calculateDiseaseFactor(String diseases) {
        if (diseases == null || diseases.trim().isEmpty()) return 1.0;

        String d = diseases.toLowerCase();

        // ── Critical / Very High Risk (×1.8) ──
        if (containsAny(d, "cancer", "tumor", "hiv", "aids",
                "organ failure", "transplant", "kidney failure",
                "liver failure", "heart failure")) {
            return 1.8;
        }

        // ── High Risk (×1.5) ──
        if (containsAny(d, "heart", "cardiac", "stroke",
                "paralysis", "parkinson", "alzheimer",
                "multiple sclerosis", "ms ", "crohn",
                "lupus", "cirrhosis", "chronic kidney")) {
            return 1.5;
        }

        // ── Medium-High Risk (×1.35) ──
        if (containsAny(d, "diabetes", "diabetic",
                "hypertension", "blood pressure", "bp",
                "copd", "asthma", "epilepsy", "seizure",
                "thyroid", "obesity", "arthritis")) {
            return 1.35;
        }

        // ── Medium Risk (×1.2) ──
        if (containsAny(d, "cholesterol", "fatty liver",
                "gastric", "ulcer", "anxiety", "depression",
                "migraine", "psoriasis", "eczema",
                "back pain", "spine")) {
            return 1.2;
        }

        // ── Low Risk / Unknown condition (×1.1) ──
        return 1.1; // any other condition mentioned
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    // =================== PLANS ===================

    public List<InsurancePlanResponse> getAvailablePlans() {
        return insurancePlanService.getAllActivePlans();
    }

    // =================== MAPPER ===================

    private PolicyResponse mapToResponse(Policy policy) {
        List<PolicyMemberResponse> memberResponses = new ArrayList<>();
        if (policy.getMembers() != null) {
            memberResponses = policy.getMembers().stream()
                    .map(member -> PolicyMemberResponse.builder()
                            .memberId(member.getMemberId())
                            .memberName(member.getMemberName())
                            .relationship(member.getRelationship() != null ? member.getRelationship().name() : null)
                            .dateOfBirth(member.getDateOfBirth())
                            .gender(member.getGender() != null ? member.getGender().name() : null)
                            .preExistingDiseases(member.getPreExistingDiseases())
                            .build())
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
                .assignedAt(policy.getAssignedAt());

        if (policy.getAssignedUnderwriter() != null) {
            builder.underwriterId(policy.getAssignedUnderwriter().getUserId())
                    .underwriterName(policy.getAssignedUnderwriter().getFirstName()
                            + " " + policy.getAssignedUnderwriter().getLastName());
        }

        builder.underwriterRemarks(policy.getUnderwriterRemarks());

        if (policy.getOriginalPolicy() != null) {
            builder.originalPolicyId(policy.getOriginalPolicy().getPolicyId());
        }

        return builder.build();
    }

    // =================== RAISE CONCERN ===================

    @Transactional
    public void raiseConcern(Long underwriterId, Long policyId, String remarks) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        Underwriter underwriter = policy.getAssignedUnderwriter();
        if (underwriter == null || !underwriter.getUserId().equals(underwriterId)) {
            throw new UnauthorizedException("This policy is not assigned to you");
        }

        if (policy.getPolicyStatus() != PolicyStatus.ASSIGNED) {
            throw new BadRequestException("Policy is not in ASSIGNED state");
        }

        policy.setPolicyStatus(PolicyStatus.CONCERN_RAISED);
        policy.setUnderwriterRemarks(remarks);
        policyRepository.save(policy);

        // ── NEW: Notify customer ──
        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "⚠️ A concern has been raised for your policy " + policy.getPolicyNumber()
                        + ". Please log in to review and reapply.",
                NotificationType.GENERAL
        );
        emailService.sendStatusChangeEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(), "CONCERN_RAISED"
        );
        auditLogService.log("CONCERN_RAISED", "UNDERWRITER", underwriter.getEmail(),
                "Concern raised for policy " + policy.getPolicyNumber()
                        + ". Remarks: " + remarks);
        log.info("Underwriter {} raised concern for policy {}. Remarks: {}", underwriterId, policyId, remarks);
    }
}
