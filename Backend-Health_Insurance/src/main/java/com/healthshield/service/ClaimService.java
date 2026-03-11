package com.healthshield.service;

import com.healthshield.dto.request.ClaimRequest;
import com.healthshield.dto.request.ClaimStatusUpdateRequest;
import com.healthshield.dto.response.ClaimDocumentResponse;
import com.healthshield.dto.response.ClaimResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.ClaimType;
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
//    private final AuditService auditService;

    @Value("${file.upload.dir:uploads/claims}")
    private String uploadDir;

    @Transactional
    public ClaimResponse fileClaim(Long userId, ClaimRequest request, List<MultipartFile> documents) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

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
                .isEscalated(false)
                .documents(new ArrayList<>())
                .build();

        Claim saved = claimRepository.save(claim);

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
                }
            } catch (IOException e) {
                throw new BadRequestException("Could not store documents: " + e.getMessage());
            }
        }

//        // Audit trail
//        auditService.logCreation("CLAIM", saved.getClaimId(),
//                "Claim filed: " + claimNumber + " | Amount: ₹" + request.getClaimAmount()
//                        + " | Hospital: " + request.getHospitalName()
//                        + " | Diagnosis: " + request.getDiagnosis(), user);
//
//        log.info("New claim filed: {} by user {} for policy {}", claimNumber, userId, policy.getPolicyNumber());
//
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
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (request.getStatus() != null) {
            claim.setClaimStatus(request.getStatus());
        }
        if (request.getApprovedAmount() != null) {
            claim.setApprovedAmount(request.getApprovedAmount());
        }
        if (request.getRejectionReason() != null) {
            claim.setRejectionReason(request.getRejectionReason());
        }

        Claim saved = claimRepository.save(claim);
        return mapToResponse(saved);
    }

    /**
     * Settle a claim — process the actual money disbursement.
     * Called after a claim is approved/partially approved.
     */
    @Transactional
    public ClaimResponse settleClaim(Long claimId, User performedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (claim.getClaimStatus() != ClaimStatus.APPROVED
                && claim.getClaimStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new BadRequestException("Only APPROVED or PARTIALLY_APPROVED claims can be settled. Current: " + claim.getClaimStatus());
        }

        BigDecimal settlementAmount = claim.getApprovedAmount();
        claim.setClaimStatus(ClaimStatus.SETTLED);
        claim.setSettlementAmount(settlementAmount);
        claim.setSettlementDate(java.time.LocalDate.now());
        claim.setTpaReferenceNumber("TPA-" + Year.now().getValue() + "-" + String.format("%08d", new Random().nextInt(99999999)));

        // Update policy's remaining coverage
        Policy policy = claim.getPolicy();
        BigDecimal totalClaimed = policy.getTotalClaimedAmount() != null
                ? policy.getTotalClaimedAmount() : BigDecimal.ZERO;
        policy.setTotalClaimedAmount(totalClaimed.add(settlementAmount));
        policy.setRemainingCoverage(policy.getCoverageAmount().subtract(policy.getTotalClaimedAmount()));
        policyRepository.save(policy);

        Claim saved = claimRepository.save(claim);

//        auditService.logStatusChange("CLAIM", claimId,
//                "APPROVED", "SETTLED",
//                "Claim settled for ₹" + settlementAmount + " | TPA Ref: " + claim.getTpaReferenceNumber(),
//                performedBy);
//
//        log.info("Claim {} settled for ₹{}", claim.getClaimNumber(), settlementAmount);

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
                .reviewStartedAt(claim.getReviewStartedAt())
                .reviewedAt(claim.getReviewedAt())
                .reviewerRemarks(claim.getReviewerRemarks())
                .isEscalated(claim.getIsEscalated())
                // .escalationReason(claim.getEscalationReason() != null ? claim.getEscalationReason().name() : null) // ESCALATION COMMENTED OUT
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
