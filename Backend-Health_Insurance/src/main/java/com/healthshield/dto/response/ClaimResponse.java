package com.healthshield.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
    private Long claimId;
    private String claimNumber;
    private Long policyId;
    private String policyNumber;
    private Long customerId;
    private String customerName;
    private String claimType;
    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;
    private BigDecimal settlementAmount;
    private String hospitalName;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String diagnosis;
    private String claimStatus;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private List<ClaimDocumentResponse> documents;

    // AI Extraction Report
    private BigDecimal extractedAmount;
    private Boolean isSuspicious;
    private Boolean isAmountMatch;
    private String extractionFlags;

    // Claims Officer info
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private LocalDateTime reviewStartedAt;
    private LocalDateTime reviewedAt;
    private String reviewerRemarks;

    // Escalation info - COMMENTED OUT
    // private Boolean isEscalated;
    // private String escalationReason;
    // private String escalationNotes;
    // private LocalDateTime escalatedAt;
    // private String adminRemarks;
    // private LocalDateTime escalationResolvedAt;

    // Settlement info
    private LocalDate settlementDate;
    private String tpaReferenceNumber;

    // Customer Bank Details
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
    private String bankName;
}
