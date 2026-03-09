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
    private List<PolicyMemberResponse> members;

    // Agent tracking
    private Long agentId;
    private String agentName;
    private BigDecimal commissionAmount;

    // Renewal tracking
    private Integer renewalCount;
    private BigDecimal noClaimBonus;
    private Long originalPolicyId;
}
