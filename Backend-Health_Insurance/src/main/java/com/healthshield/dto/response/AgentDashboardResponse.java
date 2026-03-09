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
public class AgentDashboardResponse {
    private Long agentId;
    private String agentName;
    private String email;
    private String licenseNumber;
    private String territory;
    private BigDecimal commissionPercentage;
    private Integer totalPoliciesSold;
    private BigDecimal totalCommissionEarned;
    private Long totalCustomersServed;
    private Long activePolicies;
    private BigDecimal totalPremiumGenerated;
}
