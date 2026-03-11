package com.healthshield.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminAssignClaimsOfficerRequest {

    @NotNull(message = "Claims Officer ID is required")
    private Long claimsOfficerId;
}
