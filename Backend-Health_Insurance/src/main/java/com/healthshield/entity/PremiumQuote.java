package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "premium_quotes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private InsurancePlan plan;

    private Integer age;
    private Boolean smoker;
    private Boolean preExistingDiseases;

    @Column(columnDefinition = "integer default 1")
    private Integer numberOfMembers = 1;

    private BigDecimal calculatedPremium;

    @CreationTimestamp
    private LocalDateTime calculatedAt;
}

