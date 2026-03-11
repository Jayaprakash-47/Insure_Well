package com.healthshield.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PolicyPurchaseRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    private Long quoteId;

    @NotBlank(message = "Nominee name is required")
    private String nomineeName;

    @NotBlank(message = "Nominee relationship is required")
    private String nomineeRelationship;

    @Valid
    private List<PolicyMemberRequest> members;
}
