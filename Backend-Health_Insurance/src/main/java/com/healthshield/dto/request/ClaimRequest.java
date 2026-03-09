package com.healthshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ClaimRequest {

    @NotNull(message = "Policy ID is required")
    private Long policyId;

    @NotBlank(message = "Claim type is required")
    private String claimType;

    @NotNull(message = "Claim amount is required")
    private BigDecimal claimAmount;

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String diagnosis;
}
