package com.healthshield.entity;

import com.healthshield.enums.Gender;
import com.healthshield.enums.Relationship;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "policy_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    private String memberName;

    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String preExistingDiseases;
}

