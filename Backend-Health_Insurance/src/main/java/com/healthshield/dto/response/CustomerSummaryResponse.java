package com.healthshield.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerSummaryResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String dateOfBirth;  // formatted string
    private int totalPolicies;
}