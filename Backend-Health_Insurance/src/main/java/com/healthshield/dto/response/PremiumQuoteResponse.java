package com.healthshield.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumQuoteResponse {
    private Long quoteId;
    private String planName;
    private Integer age;
    private Boolean smoker;
    private Boolean preExistingDiseases;
    private Integer numberOfMembers;
    private BigDecimal calculatedPremium;
    private LocalDateTime calculatedAt;
}

