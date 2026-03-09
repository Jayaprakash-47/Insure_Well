package com.healthshield.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long auditId;
    private String entityType;
    private Long entityId;
    private String action;
    private String previousValue;
    private String newValue;
    private String remarks;
    private String performedByName;
    private LocalDateTime timestamp;
}
