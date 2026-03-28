package com.healthshield.dto;

import lombok.Data;

@Data
public class AiAuditResult {
    private Double extractedAmount;
    private Double claimedAmount;
    private Boolean amountMatch;
    private String hospitalName;
    private String patientName;
    private String admissionDate;
    private String dischargeDate;
    private String diagnosis;
    private String confidence; // HIGH, MEDIUM, LOW
}
