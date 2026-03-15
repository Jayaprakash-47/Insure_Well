package com.healthshield.controller;

import com.healthshield.entity.AuditLog;
import com.healthshield.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // Admin sees all logs
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    // Filter by role
    @GetMapping("/role/{role}")
    public ResponseEntity<List<AuditLog>> getByRole(@PathVariable String role) {
        return ResponseEntity.ok(auditLogService.getLogsByRole(role));
    }

    // Each user sees their own logs
    @GetMapping("/my")
    public ResponseEntity<List<AuditLog>> getMyLogs(Authentication auth) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(auth.getName()));
    }
}