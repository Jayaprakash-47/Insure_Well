package com.healthshield.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminAssignUnderwriterRequest {

    @NotNull(message = "Underwriter ID is required")
    private Long underwriterId;
}
