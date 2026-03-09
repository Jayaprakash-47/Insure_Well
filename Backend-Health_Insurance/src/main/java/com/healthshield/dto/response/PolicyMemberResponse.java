package com.healthshield.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyMemberResponse {
    private Long memberId;
    private String memberName;
    private String relationship;
    private LocalDate dateOfBirth;
    private String gender;
    private String preExistingDiseases;
}

