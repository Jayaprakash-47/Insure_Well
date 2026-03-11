//package com.healthshield.dto.request;
//
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.util.List;
//
///**
// * DTO for an Agent selling a policy on behalf of a customer.
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class AgentSellPolicyRequest {
//
//    @NotNull(message = "Customer ID is required")
//    private Long customerId;
//
//    @NotNull(message = "Plan ID is required")
//    private Long planId;
//
//    private String nomineeName;
//    private String nomineeRelationship;
//
//    /** Optional: premium quote ID for pre-calculated premium */
//    private Long quoteId;
//
//    /** Optional: family members to add to the policy */
//    private List<PolicyMemberRequest> members;
//}
