package com.healthshield.dto.request;

import com.healthshield.enums.ClaimStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClaimStatusUpdateRequest {

    private ClaimStatus status;
    private BigDecimal approvedAmount;
    private String rejectionReason;
}
