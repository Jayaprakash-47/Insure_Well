package com.healthshield.dto.request;

import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.EscalationReason;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Claims Officer to submit their review decision on a claim.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewRequest {

    @NotNull(message = "Decision status is required")
    private ClaimStatus decision;  // APPROVED, PARTIALLY_APPROVED, REJECTED, ESCALATED, DOCUMENT_PENDING

    /** Approved/partial amount (required for APPROVED and PARTIALLY_APPROVED) */
    private BigDecimal approvedAmount;

    /** Detailed remarks explaining the decision */
    private String reviewerRemarks;

    /** Required when decision is REJECTED */
    private String rejectionReason;

    /** Required when decision is ESCALATED */
    private EscalationReason escalationReason;

    /** Additional notes for escalation */
    private String escalationNotes;

    /** Required when decision is DOCUMENT_PENDING — what documents are needed */
    private String additionalDocumentsRequired;
}
