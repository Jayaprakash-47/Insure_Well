package com.healthshield.controller;

import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.PolicyRenewalRequest;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.User;
import com.healthshield.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchasePolicy(@AuthenticationPrincipal User user,
                                                          @Valid @RequestBody PolicyPurchaseRequest request) {
        return new ResponseEntity<>(policyService.purchasePolicy(user.getUserId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/my-policies")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(policyService.getPoliciesByUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','UNDERWRITER')")
    public ResponseEntity<PolicyResponse> getPolicyById(@AuthenticationPrincipal User user,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyById(user, id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<PolicyResponse> cancelPolicy(@AuthenticationPrincipal User user,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(policyService.cancelPolicy(user, id));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','UNDERWRITER')")
    public ResponseEntity<List<PolicyMemberResponse>> getPolicyMembers(@AuthenticationPrincipal User user,
                                                                        @PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyMembers(user, id));
    }

    /** Renew an expired/expiring policy — creates a new policy with No-Claim Bonus */
    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> renewPolicy(@AuthenticationPrincipal User user,
                                                       @PathVariable Long id,
                                                       @RequestBody PolicyRenewalRequest request) {
        return new ResponseEntity<>(policyService.renewPolicy(user, id, request), HttpStatus.CREATED);
    }
    @GetMapping("/test-expire/{policyId}")
    public ResponseEntity<String> testExpirePolicy(@PathVariable Long policyId) {
        policyService.expirePolicyForTesting(policyId);
        return ResponseEntity.ok("Policy " + policyId + " has been forced to EXPIRED state for testing.");
    }
}
