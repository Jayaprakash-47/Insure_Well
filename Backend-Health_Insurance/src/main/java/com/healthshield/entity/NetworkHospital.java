package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Network hospitals where cashless claims can be processed.
 */
@Entity
@Table(name = "network_hospitals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hospitalId;

    @Column(unique = true)
    private String hospitalCode;

    private String hospitalName;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String state;
    private String pincode;

    private String contactNumber;
    private String email;

    /** Hospital type — e.g., "Multi-Specialty", "Super-Specialty", "General" */
    private String hospitalType;

    /** NABH accreditation status */
    @Column(columnDefinition = "boolean default false")
    private Boolean nabhAccredited = false;

    @Column(columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
