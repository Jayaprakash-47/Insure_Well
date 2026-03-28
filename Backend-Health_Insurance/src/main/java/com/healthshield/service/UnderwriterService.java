package com.healthshield.service;

import com.healthshield.dto.request.UnderwriterQuoteRequest;
import com.healthshield.dto.response.CustomerSummaryResponse;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnderwriterService {

    private final UnderwriterRepository  underwriterRepository;
    private final PolicyRepository       policyRepository;
    private final PolicyMemberRepository policyMemberRepository;
    private final InsurancePlanService   insurancePlanService;
    private final NotificationService    notificationService;
    private final EmailService           emailService;
    private final AuditLogService        auditLogService;
    private final UserRepository         userRepository;
    private final CustomerRepository     customerRepository;
    // ── Delegate AI-aware quote calculation to PolicyService ──────────────
    private final PolicyService          policyService;

    // =================== DASHBOARD ===================

    public UnderwriterDashboardResponse getDashboard(Long underwriterId) {
        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Underwriter not found with id: " + underwriterId));

        List<Policy> myPolicies = policyRepository
                .findByAssignedUnderwriterUserId(underwriterId);

        long pendingAssignments = myPolicies.stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ASSIGNED)
                .count();

        BigDecimal totalCommission = myPolicies.stream()
                .filter(p -> p.getCommissionAmount() != null)
                .map(Policy::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long customersServed = myPolicies.stream()
                .map(p -> p.getUser().getUserId())
                .distinct().count();

        long activePolicies = myPolicies.stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ACTIVE)
                .count();

        BigDecimal totalPremium = myPolicies.stream()
                .map(p -> p.getPremiumAmount() != null
                        ? p.getPremiumAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UnderwriterDashboardResponse.builder()
                .underwriterId(underwriter.getUserId())
                .underwriterName(underwriter.getFirstName()
                        + " " + underwriter.getLastName())
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

    // =================== ASSIGNMENTS ===================

    public List<PolicyResponse> getPendingAssignments(Long underwriterId) {
        return policyRepository
                .findByAssignedUnderwriterUserId(underwriterId).stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.ASSIGNED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PolicyResponse> getMyPolicies(Long underwriterId) {
        return policyRepository
                .findByAssignedUnderwriterUserId(underwriterId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =================== CUSTOMERS ===================

    public List<CustomerSummaryResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> {
                    long policyCount = policyRepository
                            .findByUserUserId(c.getUserId()).size();
                    return CustomerSummaryResponse.builder()
                            .userId(c.getUserId())
                            .name(c.getFirstName() + " " + c.getLastName())
                            .email(c.getEmail())
                            .phone(c.getPhone())
                            .city(c.getCity())
                            .dateOfBirth(c.getDateOfBirth() != null
                                    ? c.getDateOfBirth().toString() : null)
                            .totalPolicies((int) policyCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // =================== CALCULATE QUOTE ===================

    /**
     * Delegates to PolicyService.calculateUnderwriterQuote which uses:
     *   - AI-extracted conditions for per-disease loading
     *   - Age factor from oldest member
     *   - Member count factor
     *
     * The old manual disease-factor logic here has been removed to avoid
     * the quote ignoring AI results.
     */
    @Transactional
    public BigDecimal calculateQuoteForPolicy(Long underwriterId, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found: " + policyId));

        if (policy.getAssignedUnderwriter() == null
                || !policy.getAssignedUnderwriter().getUserId()
                .equals(underwriterId)) {
            throw new UnauthorizedException(
                    "This policy is not assigned to you");
        }

        // ── Single source of truth for quote calculation ──────────────────
        return policyService.calculateUnderwriterQuote(policyId);
    }

    // =================== SEND QUOTE ===================

    @Transactional
    public PolicyResponse sendPremiumQuote(Long underwriterId,
                                           Long policyId,
                                           UnderwriterQuoteRequest request) {
        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Underwriter not found with id: " + underwriterId));

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with id: " + policyId));

        if (policy.getAssignedUnderwriter() == null
                || !policy.getAssignedUnderwriter().getUserId()
                .equals(underwriterId)) {
            throw new UnauthorizedException(
                    "This policy application is not assigned to you");
        }

        if (policy.getPolicyStatus() != PolicyStatus.ASSIGNED) {
            throw new BadRequestException(
                    "Only ASSIGNED policies can receive a quote. Current: "
                            + policy.getPolicyStatus());
        }

        BigDecimal commissionAmount = request.getQuoteAmount()
                .multiply(underwriter.getCommissionPercentage() != null
                        ? underwriter.getCommissionPercentage()
                        : BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        policy.setQuoteAmount(request.getQuoteAmount());
        policy.setPremiumAmount(request.getQuoteAmount());
        policy.setCommissionAmount(commissionAmount);
        policy.setPolicyStatus(PolicyStatus.QUOTE_SENT);

        underwriter.setTotalQuotesSent(underwriter.getTotalQuotesSent() + 1);
        underwriterRepository.save(underwriter);

        Policy saved = policyRepository.save(policy);

        log.info("Underwriter {} sent quote ₹{} for policy {}",
                underwriter.getLicenseNumber(),
                request.getQuoteAmount(), policy.getPolicyNumber());

        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "📨 Your premium quote for policy " + policy.getPolicyNumber()
                        + " is ₹" + request.getQuoteAmount()
                        + ". Please log in to review and make payment.",
                NotificationType.QUOTE_RECEIVED);

        emailService.sendStatusChangeEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(), "QUOTE_SENT");

        auditLogService.log("QUOTE_SENT", "UNDERWRITER", underwriter.getEmail(),
                "Quote ₹" + request.getQuoteAmount()
                        + " sent for policy " + policy.getPolicyNumber());

        return mapToResponse(saved);
    }

    // =================== PLANS ===================

    public List<InsurancePlanResponse> getAvailablePlans() {
        return insurancePlanService.getAllActivePlans();
    }

    // =================== RAISE CONCERN ===================

    @Transactional
    public void raiseConcern(Long underwriterId, Long policyId, String remarks) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Policy not found with id: " + policyId));

        Underwriter underwriter = policy.getAssignedUnderwriter();
        if (underwriter == null
                || !underwriter.getUserId().equals(underwriterId)) {
            throw new UnauthorizedException(
                    "This policy is not assigned to you");
        }

        if (policy.getPolicyStatus() != PolicyStatus.ASSIGNED) {
            throw new BadRequestException("Policy is not in ASSIGNED state");
        }

        policy.setPolicyStatus(PolicyStatus.CONCERN_RAISED);
        policy.setUnderwriterRemarks(remarks);
        policyRepository.save(policy);

        notificationService.sendNotification(
                policy.getUser().getEmail(),
                "⚠️ A concern has been raised for your policy "
                        + policy.getPolicyNumber()
                        + ". Please log in to review and reapply.",
                NotificationType.GENERAL);

        emailService.sendStatusChangeEmail(
                policy.getUser().getEmail(),
                policy.getUser().getFirstName(),
                policy.getPolicyNumber(), "CONCERN_RAISED");

        auditLogService.log("CONCERN_RAISED", "UNDERWRITER",
                underwriter.getEmail(),
                "Concern raised for policy " + policy.getPolicyNumber()
                        + ". Remarks: " + remarks);

        log.info("Underwriter {} raised concern for policy {}",
                underwriterId, policyId);
    }

    // =================== MAPPER ===================

    /**
     * Maps Policy to PolicyResponse.
     * Includes extractedConditions, aiAnalysisDone, kycStatus so the
     * underwriter pending page can display AI results and KYC state.
     */
    private PolicyResponse mapToResponse(Policy policy) {
        List<PolicyMemberResponse> memberResponses = new ArrayList<>();
        if (policy.getMembers() != null) {
            memberResponses = policy.getMembers().stream()
                    .map(member -> PolicyMemberResponse.builder()
                            .memberId(member.getMemberId())
                            .memberName(member.getMemberName())
                            .relationship(member.getRelationship() != null
                                    ? member.getRelationship().name() : null)
                            .dateOfBirth(member.getDateOfBirth())
                            .gender(member.getGender() != null
                                    ? member.getGender().name() : null)
                            .preExistingDiseases(member.getPreExistingDiseases())
                            .build())
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
                .assignedAt(policy.getAssignedAt())
                // ── AI analysis fields — required by underwriter pending page ──
                .extractedConditions(policy.getExtractedConditions())
                .aiAnalysisDone(policy.getAiAnalysisDone())
                // ── KYC status — required by underwriter pending page ──────────
                .kycStatus(policy.getKycStatus() != null
                        ? policy.getKycStatus().name() : "PENDING");

        if (policy.getAssignedUnderwriter() != null)
            builder.underwriterId(policy.getAssignedUnderwriter().getUserId())
                    .underwriterName(
                            policy.getAssignedUnderwriter().getFirstName()
                                    + " " + policy.getAssignedUnderwriter().getLastName());

        if (policy.getOriginalPolicy() != null)
            builder.originalPolicyId(policy.getOriginalPolicy().getPolicyId());

        builder.underwriterRemarks(policy.getUnderwriterRemarks());
        return builder.build();
    }
}