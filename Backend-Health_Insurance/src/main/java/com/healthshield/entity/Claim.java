package com.healthshield.entity;

import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.ClaimType;
// import com.healthshield.enums.EscalationReason; // ESCALATION COMMENTED OUT
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long claimId;

    @Column(unique = true)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ClaimType claimType;

    private BigDecimal claimAmount;
    private BigDecimal approvedAmount;

    /** Final settlement amount (may differ from approved due to co-pay, deductibles) */
    private BigDecimal settlementAmount;

    private String hospitalName;

    private LocalDate admissionDate;
    private LocalDate dischargeDate;

    private String diagnosis;

    @Enumerated(EnumType.STRING)
    private ClaimStatus claimStatus;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // =================== AI EXTRACTION REPORT ===================
    private BigDecimal extractedAmount;
    private Boolean isSuspicious;
    private Boolean isAmountMatch;
    
    @Column(columnDefinition = "TEXT")
    private String extractionFlags;

    // =================== CLAIMS OFFICER FIELDS ===================

    /** The officer assigned to review this claim */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private ClaimsOfficer assignedOfficer;

    /** When the officer picked up the claim for review */
    private LocalDateTime reviewStartedAt;

    /** When the officer made the decision */
    private LocalDateTime reviewedAt;

    /** Detailed reviewer remarks/notes */
    @Column(columnDefinition = "TEXT")
    private String reviewerRemarks;

    // =================== ESCALATION FIELDS ===================


    // =================== SETTLEMENT FIELDS ===================

    private LocalDate settlementDate;

    /** TPA (Third Party Administrator) reference number */
    private String tpaReferenceNumber;

    // =================== TIMESTAMPS ===================

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL)
    private List<ClaimDocument> documents;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
