package com.healthshield.repository;

import com.healthshield.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByTimestampDesc();
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String role);
    List<AuditLog> findByUserEmailOrderByTimestampDesc(String email);
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
}
