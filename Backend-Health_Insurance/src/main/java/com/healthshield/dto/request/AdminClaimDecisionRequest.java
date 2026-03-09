package com.healthshield.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Admin's final decision on an escalated claim.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClaimDecisionRequest {

    @NotBlank(message = "Decision status is required")
    private String decision; // APPROVED, PARTIALLY_APPROVED, REJECTED

    private java.math.BigDecimal approvedAmount;

    @NotBlank(message = "Admin remarks are required")
    private String adminRemarks;

    private String rejectionReason;
}
