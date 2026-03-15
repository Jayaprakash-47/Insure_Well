package com.healthshield.service;

import com.healthshield.entity.AuditLog;
import com.healthshield.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String action, String performedBy, String userEmail, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .userEmail(userEmail)
                .details(details)
                .build());
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsByRole(String role) {
        return auditLogRepository.findByPerformedByOrderByTimestampDesc(role);
    }

    public List<AuditLog> getLogsByUser(String email) {
        return auditLogRepository.findByUserEmailOrderByTimestampDesc(email);
    }
}