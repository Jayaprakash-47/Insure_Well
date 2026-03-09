package com.healthshield.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePlanResponse {
    private Long planId;
    private String planName;
    private String planType;
    private String description;
    private BigDecimal basePremiumAmount;
    private BigDecimal coverageAmount;
    private Integer planDurationMonths;
    private Integer minAgeLimit;
    private Integer maxAgeLimit;
    private Integer waitingPeriodMonths;
    private Boolean maternityCover;
    private Boolean preExistingDiseaseCover;
    private Boolean isActive;
}
