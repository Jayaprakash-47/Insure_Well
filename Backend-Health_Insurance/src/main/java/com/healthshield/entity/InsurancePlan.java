package com.healthshield.entity;

import com.healthshield.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "insurance_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsurancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;

    private String planName;

    @Enumerated(EnumType.STRING)
    private PlanType planType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal basePremiumAmount;
    private BigDecimal coverageAmount;

    private Integer planDurationMonths;
    private Integer minAgeLimit;
    private Integer maxAgeLimit;
    private Integer waitingPeriodMonths;

    private Boolean maternityCover;
    private Boolean preExistingDiseaseCover;

    @Column(columnDefinition = "boolean default true")
    private Boolean isActive = true;
}
