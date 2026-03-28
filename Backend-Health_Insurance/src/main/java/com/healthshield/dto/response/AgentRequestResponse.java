package com.healthshield.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AgentRequestResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String planTypeInterest;
    private String preferredTime;
    private String message;
    private String status;
    private Long underwriterId;
    private String underwriterName;
    private Long resultingPolicyId;
    private String resultingPolicyNumber;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}