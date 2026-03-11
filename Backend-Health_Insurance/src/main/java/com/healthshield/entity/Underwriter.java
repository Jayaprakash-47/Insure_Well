package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("UNDERWRITER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Underwriter extends User {

    private String licenseNumber;

    private String specialization;   // e.g. "Health & Life Insurance"

    private BigDecimal commissionPercentage;

    @Column(columnDefinition = "integer default 0")
    private Integer totalQuotesSent = 0;
}
