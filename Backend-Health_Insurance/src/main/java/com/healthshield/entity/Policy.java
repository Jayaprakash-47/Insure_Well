package com.healthshield.entity;

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

    /** The underwriter assigned by admin to calculate the quote */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter assignedUnderwriter;

    /** When admin assigned the underwriter */
    private LocalDateTime assignedAt;

    /** Premium quote amount sent by underwriter */
    private BigDecimal quoteAmount;

    private BigDecimal premiumAmount;
    private BigDecimal coverageAmount;

    /** Total amount already claimed from this policy */
    @Column(columnDefinition = "decimal(15,2) default 0")
    private BigDecimal totalClaimedAmount = BigDecimal.ZERO;

    /** Remaining coverage = coverageAmount - totalClaimedAmount (settled claims) */
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

    // =================== RENEWAL TRACKING ===================

    /** Number of times this policy has been renewed */
    @Column(columnDefinition = "integer default 0")
    private Integer renewalCount = 0;

    /** Reference to the original policy if this is a renewal */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_policy_id")
    private Policy originalPolicy;

    /** No-claim bonus percentage earned (increases each year without claims) */
    @Column(columnDefinition = "decimal(5,2) default 0")
    private BigDecimal noClaimBonus = BigDecimal.ZERO;

    /** Commission amount paid to the underwriter for this policy */
    private BigDecimal commissionAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<PolicyMember> members;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<Claim> claims;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL)
    private List<Payment> payments;
}

