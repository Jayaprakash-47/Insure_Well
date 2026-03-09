package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("CLAIMS_OFFICER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClaimsOfficer extends User {

    private String employeeId;

    private String department;

    /** Specialization area — e.g., "Cardiology", "General", "Oncology" */
    private String specialization;

    @Column(columnDefinition = "integer default 0")
    private Integer totalClaimsProcessed = 0;

    @Column(columnDefinition = "integer default 0")
    private Integer totalClaimsApproved = 0;

    @Column(columnDefinition = "integer default 0")
    private Integer totalClaimsRejected = 0;

    /** Max claim amount this officer can approve without escalation */
    @Column(columnDefinition = "decimal(15,2) default 500000.00")
    private java.math.BigDecimal approvalLimit = new java.math.BigDecimal("500000.00");
}
