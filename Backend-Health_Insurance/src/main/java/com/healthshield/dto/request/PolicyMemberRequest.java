package com.healthshield.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PolicyMemberRequest {

    @NotBlank(message = "Member name is required")
    private String memberName;

    @NotBlank(message = "Relationship is required")
    private String relationship;

    private LocalDate dateOfBirth;
    private String gender;
    private String preExistingDiseases;
}

