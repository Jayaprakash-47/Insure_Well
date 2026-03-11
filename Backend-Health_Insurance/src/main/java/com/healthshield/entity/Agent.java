/*
package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("AGENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Agent extends User {

    private String licenseNumber;
    private String territory;
    private BigDecimal commissionPercentage;

    @Column(columnDefinition = "integer default 0")
    private Integer totalPoliciesSold = 0;
}
*/
