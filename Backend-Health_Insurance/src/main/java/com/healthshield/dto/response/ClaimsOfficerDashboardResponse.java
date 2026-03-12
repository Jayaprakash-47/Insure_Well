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
public class ClaimsOfficerDashboardResponse {
    private Long officerId;
    private String officerName;
    private String email;
    private String employeeId;
    private String department;
    private String specialization;
    private BigDecimal approvalLimit;

    // Performance stats
    private Integer totalClaimsProcessed;
    private Integer totalClaimsApproved;
    private Integer totalClaimsRejected;
    private Double approvalRate; // percentage

    // Current workload
    private Long pendingReviewCount;    // Claims assigned but not yet reviewed
    private Long unassignedClaimCount;  // Claims in queue waiting assignment
    // private Long escalatedCount;        // Claims this officer escalated - ESCALATION COMMENTED OUT
}
