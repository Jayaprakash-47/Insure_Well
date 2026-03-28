package com.healthshield.service;

import com.healthshield.dto.request.ClaimRequest;
import com.healthshield.dto.request.ClaimStatusUpdateRequest;
import com.healthshield.dto.response.ClaimDocumentResponse;
import com.healthshield.dto.response.ClaimResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.ClaimType;
import com.healthshield.enums.NotificationType;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.ClaimDocumentRepository;
import com.healthshield.repository.ClaimRepository;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.healthshield.enums.DocumentType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final PdfExtractionService pdfExtractionService;

    @Value("${file.upload.dir:uploads/claims}")
    private String uploadDir;

    @Transactional
    public ClaimResponse fileClaim(Long userId, ClaimRequest request, List<MultipartFile> documents) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // ── ENFORCE BANK DETAILS FOR SETTLEMENT ──
        if (user instanceof com.healthshield.entity.Customer customer) {
            if (customer.getAccountNumber() == null || customer.getAccountNumber().isBlank() ||
                customer.getIfscCode() == null || customer.getIfscCode().isBlank()) {
                throw new BadRequestException("Bank details are required to file a claim. Please update your profile with account details for settlement.");
            }
        }

        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + request.getPolicyId()));

        if (!policy.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Policy does not belong to the current user");
        }

        // Only allow claims on ACTIVE policies
        if (policy.getPolicyStatus() != com.healthshield.enums.PolicyStatus.ACTIVE) {
            throw new BadRequestException("Claims can only be filed for ACTIVE policies. Current status: " + policy.getPolicyStatus());
        }

        // Validate Waiting Period
        if (policy.getStartDate() != null && policy.getPlan() != null && policy.getPlan().getWaitingPeriodMonths() > 0) {
            java.time.LocalDate claimEligibilityDate = policy.getStartDate().plusMonths(policy.getPlan().getWaitingPeriodMonths());
            if (java.time.LocalDate.now().isBefore(claimEligibilityDate)) {
                throw new BadRequestException("Claims cannot be filed within the waiting period of " 
                    + policy.getPlan().getWaitingPeriodMonths() + " months. You will be eligible to file a claim on " 
                    + claimEligibilityDate + ".");
            }
        }

        // Validate claim amount doesn't exceed total coverage
        if (request.getClaimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            throw new BadRequestException("Claim amount (₹" + request.getClaimAmount() +
                    ") exceeds total policy coverage (₹" + policy.getCoverageAmount() + ")");
        }

        // Validate claim amount doesn't exceed remaining coverage
        BigDecimal remainingCoverage = policy.getRemainingCoverage() != null
                ? policy.getRemainingCoverage() : policy.getCoverageAmount();
        if (request.getClaimAmount().compareTo(remainingCoverage) > 0) {
            throw new BadRequestException("Claim amount (₹" + request.getClaimAmount() +
                    ") exceeds remaining coverage (₹" + remainingCoverage +
                    "). Already claimed: ₹" + (policy.getTotalClaimedAmount() != null ? policy.getTotalClaimedAmount() : BigDecimal.ZERO));
        }

        String claimNumber = generateClaimNumber();

        Claim claim = Claim.builder()
                .claimNumber(claimNumber)
                .policy(policy)
                .user(user)
                .claimType(ClaimType.valueOf(request.getClaimType().toUpperCase()))
                .claimAmount(request.getClaimAmount())
                .hospitalName(request.getHospitalName())
                .admissionDate(request.getAdmissionDate())
                .dischargeDate(request.getDischargeDate())
                .diagnosis(request.getDiagnosis())
                .claimStatus(ClaimStatus.SUBMITTED)
                .documents(new ArrayList<>())
                .build();

        Claim saved = claimRepository.save(claim);

        BigDecimal highestExtracted = BigDecimal.ZERO;
        boolean pdfFound = false;

        if (documents != null && !documents.isEmpty()) {
            String claimUploadDir = uploadDir + "/" + claimNumber;
            Path uploadPath = Paths.get(claimUploadDir);
            try {
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                for (MultipartFile file : documents) {
                    if (file.isEmpty()) continue;

                    String fileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    ClaimDocument doc = ClaimDocument.builder()
                            .claim(saved)
                            .documentType(DocumentType.OTHER)
                            .fileName(fileName)
                            .filePath(filePath.toString())
                            .build();
                    claimDocumentRepository.save(doc);
                    saved.getDocuments().add(doc);

                    // ── PDF Extraction Logic ──
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        BigDecimal extracted = pdfExtractionService.extractHighestAmount(filePath);
                        if (extracted != null) {
                            pdfFound = true;
                            if (extracted.compareTo(highestExtracted) > 0) {
                                highestExtracted = extracted;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new BadRequestException("Could not store documents: " + e.getMessage());
            }
        }

        // ── AI Verification logic ──
        if (pdfFound) {
            saved.setExtractedAmount(highestExtracted);
            boolean match = (highestExtracted.compareTo(request.getClaimAmount()) == 0);
            saved.setIsAmountMatch(match);
            
            if (highestExtracted.compareTo(request.getClaimAmount()) < 0) {
                saved.setIsSuspicious(true);
                saved.setExtractionFlags("Suspicious: Extracted bill amount (₹" + highestExtracted + 
                                         ") is lower than claimed amount (₹" + request.getClaimAmount() + "). Possible over-claiming.");
            } else if (match) {
                saved.setIsSuspicious(false);
                saved.setExtractionFlags("Amount matches bill: ₹" + highestExtracted + ". Recommended for Auto-Approval.");
            } else {
                // Extracted > Claimed (Partial claim case)
                saved.setIsSuspicious(false);
                saved.setExtractionFlags("Partial Claim: Extracted bill amount (₹" + highestExtracted + 
                                         ") is higher than claimed amount (₹" + request.getClaimAmount() + "). Check if co-pay applies.");
            }
        } else {
            saved.setExtractionFlags("No parseable PDF bill found for automatic verification.");
            saved.setIsAmountMatch(false);
            saved.setIsSuspicious(false);
        }
        
        saved = claimRepository.save(saved);

        notificationService.sendNotification(
                user.getEmail(),
                "Your claim " + claimNumber + " for ₹" + request.getClaimAmount()
                        + " has been submitted successfully.",
                NotificationType.CLAIM_SUBMITTED
        );

        // Notify Admins
        List<User> admins = userRepository.findAllAdmins();
        for (User admin : admins) {
            notificationService.sendNotification(
                    admin.getEmail(),
                    "New Claim Filed: " + claimNumber + " by " + user.getFirstName() + " for ₹" + request.getClaimAmount(),
                    NotificationType.CLAIM_SUBMITTED
            );
        }

        auditLogService.log("CLAIM_SUBMITTED", "CUSTOMER", user.getEmail(),
                "Claim " + claimNumber + " filed for policy "
                        + policy.getPolicyNumber());

        return mapToResponse(saved);
    }

    public List<ClaimResponse> getClaimsByUser(Long userId) {
        return claimRepository.findByUserUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ClaimResponse getClaimById(User currentUser, Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (!isAuthorized(currentUser, claim)) {
            throw new UnauthorizedException("You are not authorized to view this claim");
        }

        return mapToResponse(claim);
    }

    public List<ClaimResponse> getAllClaims() {
        return claimRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getPendingClaims() {
        return claimRepository.findByClaimStatus(ClaimStatus.SUBMITTED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ClaimResponse> getClaimsByStatus(String status) {
        ClaimStatus claimStatus = ClaimStatus.valueOf(status.toUpperCase());
        return claimRepository.findByClaimStatus(claimStatus).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClaimResponse updateClaimStatus(Long claimId, ClaimStatusUpdateRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Claim not found with id: " + claimId));

        if (request.getStatus() != null) claim.setClaimStatus(request.getStatus());
        if (request.getApprovedAmount() != null) claim.setApprovedAmount(request.getApprovedAmount());
        if (request.getRejectionReason() != null) claim.setRejectionReason(request.getRejectionReason());

        // ── NEW: Set review timestamps ──
        if (request.getStatus() == ClaimStatus.UNDER_REVIEW
                && claim.getReviewStartedAt() == null) {
            claim.setReviewStartedAt(LocalDateTime.now());
        }
        if (request.getStatus() == ClaimStatus.APPROVED
                || request.getStatus() == ClaimStatus.PARTIALLY_APPROVED
                || request.getStatus() == ClaimStatus.REJECTED) {
            claim.setReviewedAt(LocalDateTime.now());
        }
        if (request.getReviewerRemarks() != null) {
            claim.setReviewerRemarks(request.getReviewerRemarks());
        }

        Claim saved = claimRepository.save(claim);

        // ── NEW: Update remaining coverage on APPROVED / PARTIALLY_APPROVED ──
        if (saved.getClaimStatus() == ClaimStatus.APPROVED
                || saved.getClaimStatus() == ClaimStatus.PARTIALLY_APPROVED) {

            Policy policy = saved.getPolicy();
            BigDecimal approvedAmount = saved.getApprovedAmount() != null
                    ? saved.getApprovedAmount() : BigDecimal.ZERO;

            // Recalculate remaining = coverage - all approved/settled claims on this policy
            BigDecimal totalApproved = claimRepository
                    .findByPolicyPolicyId(policy.getPolicyId())
                    .stream()
                    .filter(c -> c.getClaimStatus() == ClaimStatus.APPROVED
                            || c.getClaimStatus() == ClaimStatus.PARTIALLY_APPROVED
                            || c.getClaimStatus() == ClaimStatus.SETTLED)
                    .map(c -> c.getApprovedAmount() != null
                            ? c.getApprovedAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal newRemaining = policy.getCoverageAmount()
                    .subtract(totalApproved)
                    .max(BigDecimal.ZERO);

            policy.setRemainingCoverage(newRemaining);
            policy.setTotalClaimedAmount(totalApproved);
            policyRepository.save(policy);

            log.info("Coverage updated for policy {}: remaining = ₹{}",
                    policy.getPolicyNumber(), newRemaining);
        }

        // ── existing: Notify customer on status change (unchanged) ──
        String customerEmail = claim.getUser().getEmail();
        String customerName  = claim.getUser().getFirstName();
        String claimNo       = claim.getClaimNumber();

        switch (saved.getClaimStatus()) {
            case APPROVED, PARTIALLY_APPROVED -> {
                notificationService.sendNotification(customerEmail,
                        "Your claim " + claimNo + " has been "
                                + saved.getClaimStatus().name().replace("_", " ")
                                + ". Approved amount: ₹" + saved.getApprovedAmount(),
                        NotificationType.CLAIM_APPROVED);
                emailService.sendStatusChangeEmail(customerEmail, customerName,
                        claimNo, saved.getClaimStatus().name());
                auditLogService.log("CLAIM_APPROVED", "CLAIMS_OFFICER",
                        customerEmail, "Claim " + claimNo + " approved. "
                                + "Amount: ₹" + saved.getApprovedAmount());
            }
            case REJECTED -> {
                notificationService.sendNotification(customerEmail,
                        "Your claim " + claimNo + " has been rejected. Reason: "
                                + saved.getRejectionReason(),
                        NotificationType.CLAIM_REJECTED);
                emailService.sendStatusChangeEmail(customerEmail, customerName,
                        claimNo, "REJECTED");
                auditLogService.log("CLAIM_REJECTED", "CLAIMS_OFFICER",
                        customerEmail, "Claim " + claimNo + " rejected. Reason: "
                                + saved.getRejectionReason());
            }
            case UNDER_REVIEW -> {
                notificationService.sendNotification(customerEmail,
                        "Your claim " + claimNo + " is now under review by our team.",
                        NotificationType.GENERAL);
                auditLogService.log("CLAIM_UNDER_REVIEW", "CLAIMS_OFFICER",
                        customerEmail, "Claim " + claimNo + " moved to review");
            }
            default -> {}
        }

        return mapToResponse(saved);
    }

    @Transactional
    public ClaimResponse settleClaim(Long claimId, User performedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Claim not found with id: " + claimId));

        if (claim.getClaimStatus() != ClaimStatus.APPROVED
                && claim.getClaimStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new BadRequestException("Only APPROVED or PARTIALLY_APPROVED claims can be settled.");
        }

        BigDecimal settlementAmount = claim.getApprovedAmount();
        claim.setClaimStatus(ClaimStatus.SETTLED);
        claim.setSettlementAmount(settlementAmount);
        claim.setSettlementDate(java.time.LocalDate.now());
        claim.setTpaReferenceNumber("TPA-" + Year.now().getValue() + "-"
                + String.format("%08d", new Random().nextInt(99999999)));

        Policy policy = claim.getPolicy();
        BigDecimal totalClaimed = policy.getTotalClaimedAmount() != null
                ? policy.getTotalClaimedAmount() : BigDecimal.ZERO;
        policy.setTotalClaimedAmount(totalClaimed.add(settlementAmount));
        policy.setRemainingCoverage(
                policy.getCoverageAmount().subtract(policy.getTotalClaimedAmount()));
        policyRepository.save(policy);

        Claim saved = claimRepository.save(claim);

        // ── NEW: Notify customer on settlement ──
        notificationService.sendNotification(
                claim.getUser().getEmail(),
                "Your claim " + claim.getClaimNumber() + " has been settled. "
                        + "Amount ₹" + settlementAmount + " will be credited within 3-5 business days.",
                NotificationType.CLAIM_APPROVED
        );
        emailService.sendStatusChangeEmail(
                claim.getUser().getEmail(),
                claim.getUser().getFirstName(),
                claim.getClaimNumber(), "SETTLED"
        );
        auditLogService.log("CLAIM_SETTLED", "CLAIMS_OFFICER",
                claim.getUser().getEmail(),
                "Claim " + claim.getClaimNumber()
                        + " settled. Amount: ₹" + settlementAmount);

        return mapToResponse(saved);
    }

    public List<ClaimDocumentResponse> getClaimDocuments(User currentUser, Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (!isAuthorized(currentUser, claim)) {
            throw new UnauthorizedException("You are not authorized to view documents for this claim");
        }

        return claimDocumentRepository.findByClaimClaimId(claimId).stream()
                .map(this::mapDocumentToResponse)
                .collect(Collectors.toList());
    }

    private String generateClaimNumber() {
        Random random = new Random();
        String number;
        do {
            number = "CLM-" + Year.now().getValue() + "-" + String.format("%06d", random.nextInt(999999));
        } while (claimRepository.existsByClaimNumber(number));
        return number;
    }

    /**
     * Check if user is authorized — owner, admin, or claims officer.
     */
    private boolean isAuthorized(User user, Claim claim) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isClaimsOfficer = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLAIMS_OFFICER"));
        boolean isOwner = claim.getUser().getUserId().equals(user.getUserId());

        return isAdmin || isClaimsOfficer || isOwner;
    }

    private ClaimResponse mapToResponse(Claim claim) {
        List<ClaimDocumentResponse> docResponses = new ArrayList<>();
        if (claim.getDocuments() != null) {
            docResponses = claim.getDocuments().stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList());
        }

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
                .documents(docResponses)
                .extractedAmount(claim.getExtractedAmount())
                .isAmountMatch(claim.getIsAmountMatch())
                .isSuspicious(claim.getIsSuspicious())
                .extractionFlags(claim.getExtractionFlags())
                .reviewStartedAt(claim.getReviewStartedAt())
                .reviewedAt(claim.getReviewedAt())
                .reviewerRemarks(claim.getReviewerRemarks())
                .settlementDate(claim.getSettlementDate())
                .tpaReferenceNumber(claim.getTpaReferenceNumber());

        if (claim.getUser() instanceof com.healthshield.entity.Customer customer) {
            builder.accountNumber(customer.getAccountNumber())
                   .ifscCode(customer.getIfscCode())
                   .accountHolderName(customer.getAccountHolderName())
                   .bankName(customer.getBankName());
        }

        if (claim.getAssignedOfficer() != null) {
            builder.assignedOfficerId(claim.getAssignedOfficer().getUserId())
                    .assignedOfficerName(claim.getAssignedOfficer().getFirstName() + " " + claim.getAssignedOfficer().getLastName());
        }

        return builder.build();
    }

    private ClaimDocumentResponse mapDocumentToResponse(ClaimDocument doc) {
        return ClaimDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .documentType(doc.getDocumentType().name())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}
