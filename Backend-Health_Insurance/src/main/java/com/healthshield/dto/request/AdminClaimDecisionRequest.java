package com.healthshield.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClaimDecisionRequest {

    @NotBlank(message = "Decision status is required")
    private String decision;

    private java.math.BigDecimal approvedAmount;

    @NotBlank(message = "Admin remarks are required")
    private String adminRemarks;

    private String rejectionReason;
}
