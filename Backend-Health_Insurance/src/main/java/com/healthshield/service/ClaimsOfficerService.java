package com.healthshield.service;

import com.healthshield.dto.request.ClaimReviewRequest;
import com.healthshield.dto.response.ClaimResponse;
import com.healthshield.dto.response.ClaimsOfficerDashboardResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.ClaimStatus;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.ClaimRepository;
import com.healthshield.repository.ClaimsOfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service handling all Claims Officer operations.
 * Implements the real-world claim review workflow with business rule validations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimsOfficerService {

    private final ClaimsOfficerRepository claimsOfficerRepository;
    private final ClaimRepository claimRepository;
    private final AuditService auditService;

    // =================== DASHBOARD ===================

    public ClaimsOfficerDashboardResponse getDashboard(Long officerId) {
        ClaimsOfficer officer = claimsOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claims Officer not found with id: " + officerId));

        long pendingReviewCount = claimRepository.findByAssignedOfficerUserIdAndClaimStatus(
                officerId, ClaimStatus.UNDER_REVIEW).size();
        long unassignedCount = claimRepository.findByAssignedOfficerIsNullAndClaimStatus(
                ClaimStatus.SUBMITTED).size();

        long escalatedByOfficer = claimRepository.findByAssignedOfficerUserId(officerId).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsEscalated()))
                .count();

        int totalProcessed = officer.getTotalClaimsProcessed() != null ? officer.getTotalClaimsProcessed() : 0;
        int totalApproved = officer.getTotalClaimsApproved() != null ? officer.getTotalClaimsApproved() : 0;
        int totalRejected = officer.getTotalClaimsRejected() != null ? officer.getTotalClaimsRejected() : 0;
        double approvalRate = totalProcessed > 0 ? (totalApproved * 100.0 / totalProcessed) : 0.0;

        return ClaimsOfficerDashboardResponse.builder()
                .officerId(officer.getUserId())
                .officerName(officer.getFirstName() + " " + officer.getLastName())
                .email(officer.getEmail())
                .employeeId(officer.getEmployeeId())
                .department(officer.getDepartment())
                .specialization(officer.getSpecialization())
                .approvalLimit(officer.getApprovalLimit())
                .totalClaimsProcessed(totalProcessed)
                .totalClaimsApproved(totalApproved)
                .totalClaimsRejected(totalRejected)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .pendingReviewCount(pendingReviewCount)
                .unassignedClaimCount(unassignedCount)
                .escalatedCount(escalatedByOfficer)
                .build();
    }

    // =================== CLAIM QUEUE ===================

    /**
     * Get unassigned claims waiting for review.
     */
    public List<ClaimResponse> getUnassignedClaims() {
        return claimRepository.findByAssignedOfficerIsNullAndClaimStatus(ClaimStatus.SUBMITTED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all claims assigned to this officer.
     */
    public List<ClaimResponse> getMyAssignedClaims(Long officerId) {
        return claimRepository.findByAssignedOfficerUserId(officerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get claims assigned to this officer filtered by status.
     */
    public List<ClaimResponse> getMyClaimsByStatus(Long officerId, String status) {
        ClaimStatus claimStatus = ClaimStatus.valueOf(status.toUpperCase());
        return claimRepository.findByAssignedOfficerUserIdAndClaimStatus(officerId, claimStatus).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get the history of claims this officer has already reviewed/decided.
     */
    public List<ClaimResponse> getMyDecisionHistory(Long officerId) {
        return claimRepository.findByAssignedOfficerUserIdAndReviewedAtIsNotNull(officerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =================== PICK UP / ASSIGN CLAIM ===================

    /**
     * Officer picks up (self-assigns) an unassigned claim from the queue.
     */
    @Transactional
    public ClaimResponse pickupClaim(Long officerId, Long claimId) {
        ClaimsOfficer officer = claimsOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claims Officer not found"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (claim.getAssignedOfficer() != null) {
            throw new BadRequestException("Claim #" + claim.getClaimNumber()
                    + " is already assigned to officer: "
                    + claim.getAssignedOfficer().getFirstName() + " " + claim.getAssignedOfficer().getLastName());
        }

        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED) {
            throw new BadRequestException("Can only pick up claims with SUBMITTED status. Current: " + claim.getClaimStatus());
        }

        claim.setAssignedOfficer(officer);
        claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);
        claim.setReviewStartedAt(LocalDateTime.now());

        Claim saved = claimRepository.save(claim);

        auditService.logStatusChange("CLAIM", claimId,
                ClaimStatus.SUBMITTED.name(), ClaimStatus.UNDER_REVIEW.name(),
                "Claim picked up for review by officer: " + officer.getFirstName() + " " + officer.getLastName(),
                officer);

        log.info("Claim {} picked up by officer {}", claim.getClaimNumber(), officer.getEmployeeId());
        return mapToResponse(saved);
    }

    // =================== REVIEW / DECISION ===================

    /**
     * Submit a review decision for a claim.
     * This is the core business logic with extensive validation.
     */
    @Transactional
    public ClaimResponse reviewClaim(Long officerId, Long claimId, ClaimReviewRequest request) {
        ClaimsOfficer officer = claimsOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claims Officer not found"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        // Validation: Claim must be assigned to this officer
        if (claim.getAssignedOfficer() == null || !claim.getAssignedOfficer().getUserId().equals(officerId)) {
            throw new UnauthorizedException("This claim is not assigned to you");
        }

        // Validation: Claim must be in reviewable state
        if (claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW
                && claim.getClaimStatus() != ClaimStatus.DOCUMENT_PENDING) {
            throw new BadRequestException("Claim is not in a reviewable state. Current: " + claim.getClaimStatus());
        }

        String previousStatus = claim.getClaimStatus().name();

        // Process based on decision type
        switch (request.getDecision()) {
            case APPROVED:
                processApproval(claim, officer, request);
                break;
            case PARTIALLY_APPROVED:
                processPartialApproval(claim, officer, request);
                break;
            case REJECTED:
                processRejection(claim, officer, request);
                break;
            case ESCALATED:
                processEscalation(claim, officer, request);
                break;
            case DOCUMENT_PENDING:
                processDocumentRequest(claim, officer, request);
                break;
            default:
                throw new BadRequestException("Invalid decision: " + request.getDecision()
                        + ". Valid: APPROVED, PARTIALLY_APPROVED, REJECTED, ESCALATED, DOCUMENT_PENDING");
        }

        Claim saved = claimRepository.save(claim);

        auditService.logStatusChange("CLAIM", claimId,
                previousStatus, claim.getClaimStatus().name(),
                "Decision: " + request.getDecision() + " | Remarks: " + request.getReviewerRemarks(),
                officer);

        log.info("Claim {} reviewed by officer {} | Decision: {}",
                claim.getClaimNumber(), officer.getEmployeeId(), request.getDecision());

        return mapToResponse(saved);
    }

    // =================== BUSINESS RULE VALIDATIONS ===================

    private void processApproval(Claim claim, ClaimsOfficer officer, ClaimReviewRequest request) {
        // Business Rule 1: Check if amount exceeds officer's approval limit
        if (claim.getClaimAmount().compareTo(officer.getApprovalLimit()) > 0) {
            throw new BadRequestException(
                    "Claim amount ₹" + claim.getClaimAmount() +
                    " exceeds your approval limit ₹" + officer.getApprovalLimit() +
                    ". Please escalate this claim to Admin.");
        }

        // Business Rule 2: Validate waiting period
        validateWaitingPeriod(claim);

        // Business Rule 3: Check remaining coverage
        validateCoverage(claim, claim.getClaimAmount());

        BigDecimal approvedAmount = request.getApprovedAmount() != null
                ? request.getApprovedAmount() : claim.getClaimAmount();

        claim.setClaimStatus(ClaimStatus.APPROVED);
        claim.setApprovedAmount(approvedAmount);
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewerRemarks(request.getReviewerRemarks());

        // Update officer stats
        officer.setTotalClaimsProcessed(officer.getTotalClaimsProcessed() + 1);
        officer.setTotalClaimsApproved(officer.getTotalClaimsApproved() + 1);
        claimsOfficerRepository.save(officer);
    }

    private void processPartialApproval(Claim claim, ClaimsOfficer officer, ClaimReviewRequest request) {
        if (request.getApprovedAmount() == null) {
            throw new BadRequestException("Approved amount is required for partial approval");
        }
        if (request.getApprovedAmount().compareTo(claim.getClaimAmount()) >= 0) {
            throw new BadRequestException("Partial approval amount must be less than claim amount. " +
                    "Use APPROVED for full approval.");
        }
        if (request.getApprovedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Approved amount must be greater than zero. " +
                    "Use REJECTED if you want to deny the claim.");
        }

        // Check officer limit
        if (request.getApprovedAmount().compareTo(officer.getApprovalLimit()) > 0) {
            throw new BadRequestException(
                    "Approved amount ₹" + request.getApprovedAmount() +
                    " exceeds your approval limit ₹" + officer.getApprovalLimit());
        }

        validateWaitingPeriod(claim);
        validateCoverage(claim, request.getApprovedAmount());

        claim.setClaimStatus(ClaimStatus.PARTIALLY_APPROVED);
        claim.setApprovedAmount(request.getApprovedAmount());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewerRemarks(request.getReviewerRemarks());

        officer.setTotalClaimsProcessed(officer.getTotalClaimsProcessed() + 1);
        officer.setTotalClaimsApproved(officer.getTotalClaimsApproved() + 1);
        claimsOfficerRepository.save(officer);
    }

    private void processRejection(Claim claim, ClaimsOfficer officer, ClaimReviewRequest request) {
        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BadRequestException("Rejection reason is mandatory when rejecting a claim");
        }

        claim.setClaimStatus(ClaimStatus.REJECTED);
        claim.setRejectionReason(request.getRejectionReason());
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewerRemarks(request.getReviewerRemarks());

        officer.setTotalClaimsProcessed(officer.getTotalClaimsProcessed() + 1);
        officer.setTotalClaimsRejected(officer.getTotalClaimsRejected() + 1);
        claimsOfficerRepository.save(officer);
    }

    private void processEscalation(Claim claim, ClaimsOfficer officer, ClaimReviewRequest request) {
        if (request.getEscalationReason() == null) {
            throw new BadRequestException("Escalation reason is required");
        }

        claim.setClaimStatus(ClaimStatus.ESCALATED);
        claim.setIsEscalated(true);
        claim.setEscalationReason(request.getEscalationReason());
        claim.setEscalationNotes(request.getEscalationNotes());
        claim.setEscalatedAt(LocalDateTime.now());
        claim.setReviewerRemarks(request.getReviewerRemarks());
    }

    private void processDocumentRequest(Claim claim, ClaimsOfficer officer, ClaimReviewRequest request) {
        claim.setClaimStatus(ClaimStatus.DOCUMENT_PENDING);
        claim.setReviewerRemarks("Additional documents required: " +
                (request.getAdditionalDocumentsRequired() != null
                        ? request.getAdditionalDocumentsRequired()
                        : request.getReviewerRemarks()));
    }

    // =================== BUSINESS RULES ===================

    /**
     * Validate that the claim is not within the policy's waiting period.
     * Real insurance companies enforce waiting periods strictly.
     */
    private void validateWaitingPeriod(Claim claim) {
        Policy policy = claim.getPolicy();
        InsurancePlan plan = policy.getPlan();

        if (plan.getWaitingPeriodMonths() != null && plan.getWaitingPeriodMonths() > 0
                && policy.getStartDate() != null) {
            LocalDate waitingPeriodEnd = policy.getStartDate().plusMonths(plan.getWaitingPeriodMonths());
            LocalDate claimDate = claim.getAdmissionDate() != null ? claim.getAdmissionDate() : LocalDate.now();

            if (claimDate.isBefore(waitingPeriodEnd)) {
                long daysRemaining = ChronoUnit.DAYS.between(claimDate, waitingPeriodEnd);
                throw new BadRequestException(
                        "⚠️ WAITING PERIOD VIOLATION: This claim falls within the " +
                        plan.getWaitingPeriodMonths() + "-month waiting period. " +
                        "Policy started: " + policy.getStartDate() +
                        ", Waiting period ends: " + waitingPeriodEnd +
                        " (" + daysRemaining + " days remaining). " +
                        "You may reject this claim or escalate to Admin for review.");
            }
        }
    }

    /**
     * Validate that the claim amount doesn't exceed remaining coverage.
     */
    private void validateCoverage(Claim claim, BigDecimal amountToApprove) {
        Policy policy = claim.getPolicy();
        BigDecimal remainingCoverage = policy.getRemainingCoverage() != null
                ? policy.getRemainingCoverage()
                : policy.getCoverageAmount();

        if (amountToApprove.compareTo(remainingCoverage) > 0) {
            throw new BadRequestException(
                    "⚠️ COVERAGE EXCEEDED: Approved amount ₹" + amountToApprove +
                    " exceeds remaining coverage ₹" + remainingCoverage +
                    " (Total coverage: ₹" + policy.getCoverageAmount() +
                    ", Already claimed: ₹" + (policy.getTotalClaimedAmount() != null ? policy.getTotalClaimedAmount() : BigDecimal.ZERO) + ")");
        }
    }

    // =================== CLAIM DETAIL ===================

    public ClaimResponse getClaimDetail(Long officerId, Long claimId) {
        ClaimsOfficer officer = claimsOfficerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Claims Officer not found"));

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        return mapToResponse(claim);
    }

    // =================== MAPPER ===================

    private ClaimResponse mapToResponse(Claim claim) {
        ClaimResponse.ClaimResponseBuilder builder = ClaimResponse.builder()
                .claimId(claim.getClaimId())
                .claimNumber(claim.getClaimNumber())
                .policyId(claim.getPolicy().getPolicyId())
                .policyNumber(claim.getPolicy().getPolicyNumber())
                .customerId(claim.getUser().getUserId())
                .customerName(claim.getUser().getFirstName() + " " + claim.getUser().getLastName())
                .claimType(claim.getClaimType().name())
                .claimAmount(claim.getClaimAmount())
                .approvedAmount(claim.getApprovedAmount())
                .settlementAmount(claim.getSettlementAmount())
                .hospitalName(claim.getHospitalName())
                .admissionDate(claim.getAdmissionDate())
                .dischargeDate(claim.getDischargeDate())
                .diagnosis(claim.getDiagnosis())
                .claimStatus(claim.getClaimStatus().name())
                .rejectionReason(claim.getRejectionReason())
                .createdAt(claim.getCreatedAt())
                .reviewStartedAt(claim.getReviewStartedAt())
                .reviewedAt(claim.getReviewedAt())
                .reviewerRemarks(claim.getReviewerRemarks())
                .isEscalated(claim.getIsEscalated())
                .escalationReason(claim.getEscalationReason() != null ? claim.getEscalationReason().name() : null)
                .escalationNotes(claim.getEscalationNotes())
                .escalatedAt(claim.getEscalatedAt())
                .adminRemarks(claim.getAdminRemarks())
                .escalationResolvedAt(claim.getEscalationResolvedAt())
                .settlementDate(claim.getSettlementDate())
                .tpaReferenceNumber(claim.getTpaReferenceNumber());

        if (claim.getAssignedOfficer() != null) {
            builder.assignedOfficerId(claim.getAssignedOfficer().getUserId())
                    .assignedOfficerName(claim.getAssignedOfficer().getFirstName() + " " + claim.getAssignedOfficer().getLastName());
        }

        if (claim.getDocuments() != null) {
            builder.documents(claim.getDocuments().stream()
                    .map(doc -> com.healthshield.dto.response.ClaimDocumentResponse.builder()
                            .documentId(doc.getDocumentId())
                            .documentType(doc.getDocumentType().name())
                            .fileName(doc.getFileName())
                            .filePath(doc.getFilePath())
                            .uploadedAt(doc.getUploadedAt())
                            .build())
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
