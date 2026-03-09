package com.healthshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InsurancePlanRequest {

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotBlank(message = "Plan type is required")
    private String planType;

    private String description;

    @NotNull(message = "Base premium amount is required")
    private BigDecimal basePremiumAmount;

    @NotNull(message = "Coverage amount is required")
    private BigDecimal coverageAmount;

    @NotNull(message = "Plan duration is required")
    private Integer planDurationMonths;

    private Integer minAgeLimit;
    private Integer maxAgeLimit;
    private Integer waitingPeriodMonths;

    private Boolean maternityCover;
    private Boolean preExistingDiseaseCover;
}
