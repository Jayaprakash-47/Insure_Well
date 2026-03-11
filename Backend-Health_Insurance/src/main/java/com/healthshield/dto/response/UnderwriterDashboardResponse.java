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
public class UnderwriterDashboardResponse {
    private Long underwriterId;
    private String underwriterName;
    private String email;
    private String licenseNumber;
    private String specialization;
    private BigDecimal commissionPercentage;
    private Integer totalQuotesSent;
    private BigDecimal totalCommissionEarned;
    private Long totalCustomersServed;
    private Long activePolicies;
    private BigDecimal totalPremiumGenerated;
    private Long pendingAssignments;
}
