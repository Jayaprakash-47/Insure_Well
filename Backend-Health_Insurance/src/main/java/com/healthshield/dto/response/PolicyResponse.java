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
public class PolicyResponse {
    private Long policyId;
    private String policyNumber;
    private Long customerId;
    private String customerName;
    private Long planId;
    private String planName;
    private BigDecimal premiumAmount;
    private BigDecimal coverageAmount;
    private BigDecimal remainingCoverage;
    private BigDecimal totalClaimedAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String policyStatus;
    private String nomineeName;
    private String nomineeRelationship;
    private LocalDateTime createdAt;
    private Integer waitingPeriodMonths;
    private List<PolicyMemberResponse> members;

    // Underwriter tracking
    private Long underwriterId;
    private String underwriterName;
    private BigDecimal commissionAmount;
    private BigDecimal quoteAmount;
    private LocalDateTime assignedAt;

    // Renewal tracking
    private Integer renewalCount;
    private BigDecimal noClaimBonus;
    private Long originalPolicyId;
}

