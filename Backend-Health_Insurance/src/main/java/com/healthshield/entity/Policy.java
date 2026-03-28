package com.healthshield.entity;

import com.healthshield.enums.KycStatus;
import com.healthshield.enums.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(unique = true)
    private String policyNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private InsurancePlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter assignedUnderwriter;

    private LocalDateTime assignedAt;

    private BigDecimal quoteAmount;
    private BigDecimal premiumAmount;
    private BigDecimal coverageAmount;

    @Column(columnDefinition = "decimal(15,2) default 0")
    private BigDecimal totalClaimedAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "decimal(15,2) default 0")
    private BigDecimal remainingCoverage = BigDecimal.ZERO;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private PolicyStatus policyStatus;

    private String nomineeName;
    private String nomineeRelationship;

    @Column(columnDefinition = "TEXT")
    private String underwriterRemarks;

    // ── KYC ─────────────────────────────────────────────────────────────
    /** Path to the uploaded Aadhaar document */
    @Column(name = "aadhaar_document_path")
    private String aadhaarDocumentPath;

    /** KYC verification status set by underwriter */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status")
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    // ── AI Health Report Analysis ────────────────────────────────────────
    /**
     * Medical conditions extracted from health report PDF by Claude AI.
     * Comma-separated: "Diabetes, Hypertension, Obesity"
     * Populated asynchronously after health report upload.
     */
    @Column(name = "extracted_conditions", columnDefinition = "TEXT")
    private String extractedConditions;

    /**
     * Whether AI analysis has been completed for this policy's health report.
     */
    @Column(name = "ai_analysis_done")
    @Builder.Default
    private Boolean aiAnalysisDone = false;

    // ── Renewal Tracking ─────────────────────────────────────────────────
    @Column(columnDefinition = "integer default 0")
    private Integer renewalCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_policy_id")
    private Policy originalPolicy;

    @Column(columnDefinition = "decimal(5,2) default 0")
    private BigDecimal noClaimBonus = BigDecimal.ZERO;

    private BigDecimal commissionAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<PolicyMember> members;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<Claim> claims;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<Payment> payments;

    @Column(name = "risk_score")
    private Integer riskScore;        // 0–100

    @Column(name = "risk_level")
    private String riskLevel;         // LOW | MEDIUM | HIGH | CRITICAL

    @Column(name = "risk_summary", columnDefinition = "TEXT")
    private String riskSummary;
}