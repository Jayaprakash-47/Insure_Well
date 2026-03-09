package com.healthshield.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PremiumCalculateRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 100, message = "Age must be at most 100")
    private Integer age;

    private Boolean smoker;
    private Boolean preExistingDiseases;
    private Integer numberOfMembers = 1;
}

