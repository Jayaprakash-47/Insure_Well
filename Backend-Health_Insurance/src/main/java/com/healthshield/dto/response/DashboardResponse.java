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
public class DashboardResponse {
    private Long totalCustomers;
    private Long totalUnderwriters;
    private Long totalClaimsOfficers;
    private Long totalAdmins;
    private Long totalPolicies;
    private Long totalActivePolicies;
    private Long totalPendingPolicies;
    private Long totalAssignedPolicies;
    private Long totalQuoteSentPolicies;
    private Long totalExpiredPolicies;
    private Long totalClaims;
    private Long totalPendingClaims;
    private Long totalUnderReviewClaims;
    private Long totalApprovedClaims;
    private Long totalRejectedClaims;
    private Long totalEscalatedClaims;
    private Long totalSettledClaims;
    private Long totalPayments;
    private BigDecimal totalRevenue;
    private BigDecimal totalClaimsPaidOut;
    private BigDecimal claimSettlementRatio;
    private Long totalActivePlans;
    private Long totalNetworkHospitals;
}

