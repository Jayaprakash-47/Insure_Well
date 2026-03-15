package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;        // e.g. POLICY_APPLIED, CLAIM_APPROVED

    @Column(nullable = false)
    private String performedBy;   // role: ADMIN, CUSTOMER, UNDERWRITER, etc.

    private String userEmail;     // who triggered it

    @Column(columnDefinition = "TEXT")
    private String details;       // human-readable description

    @CreationTimestamp
    private LocalDateTime timestamp;
}