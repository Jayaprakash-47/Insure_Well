//package com.healthshield.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//
//import java.time.LocalDateTime;
//
///**
// * Audit trail for every important state change in the system.
// * Tracks WHO did WHAT, WHEN, and on WHICH entity.
// */
//@Entity
//@Table(name = "audit_logs")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class AuditLog {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long auditId;
//
//    /** The entity type — e.g., "CLAIM", "POLICY", "USER" */
//    private String entityType;
//
//    /** The ID of the entity being changed */
//    private Long entityId;
//
//    /** What action was performed — e.g., "STATUS_CHANGE", "CREATED", "ESCALATED" */
//    private String action;
//
//    /** Previous state */
//    private String previousValue;
//
//    /** New state */
//    private String newValue;
//
//    /** Detailed remarks about the change */
//    @Column(columnDefinition = "TEXT")
//    private String remarks;
//
//    /** Who performed the action */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "performed_by")
//    private User performedBy;
//
//    /** Name cached for quick display */
//    private String performedByName;
//
//    /** IP address (optional, for security auditing) */
//    private String ipAddress;
//
//    @CreationTimestamp
//    private LocalDateTime timestamp;
//}
