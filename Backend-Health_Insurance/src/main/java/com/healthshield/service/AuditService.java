//package com.healthshield.service;
//
//import com.healthshield.dto.response.AuditLogResponse;
//import com.healthshield.entity.AuditLog;
//import com.healthshield.entity.User;
//import com.healthshield.repository.AuditLogRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Centralized audit service that logs every important state change in the system.
// * Used by all other services for regulatory compliance and traceability.
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AuditService {
//
//    private final AuditLogRepository auditLogRepository;
//
//    /**
//     * Log an audit event.
//     *
//     * @param entityType   e.g., "CLAIM", "POLICY", "USER"
//     * @param entityId     ID of the affected entity
//     * @param action       e.g., "STATUS_CHANGE", "CREATED", "ESCALATED"
//     * @param previousValue old value (can be null for creation)
//     * @param newValue      new value
//     * @param remarks       detailed description
//     * @param performedBy   the user who performed the action
//     */
//    public void logAction(String entityType, Long entityId, String action,
//                          String previousValue, String newValue,
//                          String remarks, User performedBy) {
//        AuditLog auditLog = AuditLog.builder()
//                .entityType(entityType)
//                .entityId(entityId)
//                .action(action)
//                .previousValue(previousValue)
//                .newValue(newValue)
//                .remarks(remarks)
//                .performedBy(performedBy)
//                .performedByName(performedBy != null
//                        ? performedBy.getFirstName() + " " + performedBy.getLastName()
//                        : "SYSTEM")
//                .build();
//
//        auditLogRepository.save(auditLog);
//        log.info("AUDIT: [{}] {} #{} | {} → {} | by {} | {}",
//                entityType, action, entityId, previousValue, newValue,
//                auditLog.getPerformedByName(), remarks);
//    }
//
//    /**
//     * Quick log without previous value (for creation events).
//     */
//    public void logCreation(String entityType, Long entityId, String remarks, User performedBy) {
//        logAction(entityType, entityId, "CREATED", null, "CREATED", remarks, performedBy);
//    }
//
//    /**
//     * Log a status change event.
//     */
//    public void logStatusChange(String entityType, Long entityId,
//                                String previousStatus, String newStatus,
//                                String remarks, User performedBy) {
//        logAction(entityType, entityId, "STATUS_CHANGE", previousStatus, newStatus, remarks, performedBy);
//    }
//
//    /**
//     * Get audit trail for a specific entity.
//     */
//    public List<AuditLogResponse> getAuditTrail(String entityType, Long entityId) {
//        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get all audit logs for a given entity type (e.g., all claim audits).
//     */
//    public List<AuditLogResponse> getAuditLogsByType(String entityType) {
//        return auditLogRepository.findByEntityTypeOrderByTimestampDesc(entityType)
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get recent audit logs (last 50).
//     */
//    public List<AuditLogResponse> getRecentAuditLogs() {
//        return auditLogRepository.findTop50ByOrderByTimestampDesc()
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get all actions performed by a specific user.
//     */
//    public List<AuditLogResponse> getAuditLogsByUser(Long userId) {
//        return auditLogRepository.findByPerformedByUserIdOrderByTimestampDesc(userId)
//                .stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    private AuditLogResponse mapToResponse(AuditLog log) {
//        return AuditLogResponse.builder()
//                .auditId(log.getAuditId())
//                .entityType(log.getEntityType())
//                .entityId(log.getEntityId())
//                .action(log.getAction())
//                .previousValue(log.getPreviousValue())
//                .newValue(log.getNewValue())
//                .remarks(log.getRemarks())
//                .performedByName(log.getPerformedByName())
//                .timestamp(log.getTimestamp())
//                .build();
//    }
//}
