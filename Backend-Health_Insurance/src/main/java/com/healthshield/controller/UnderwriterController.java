package com.healthshield.controller;

import com.healthshield.dto.request.UnderwriterQuoteRequest;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.dto.response.UnderwriterDashboardResponse;
import com.healthshield.entity.User;
import com.healthshield.service.UnderwriterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/underwriter")
@PreAuthorize("hasRole('UNDERWRITER')")
@RequiredArgsConstructor
public class UnderwriterController {

    private final UnderwriterService underwriterService;

    /** Dashboard: profile + stats */
    @GetMapping("/dashboard")
    public ResponseEntity<UnderwriterDashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(underwriterService.getDashboard(user.getUserId()));
    }

    /** Profile (same as dashboard) */
    @GetMapping("/profile")
    public ResponseEntity<UnderwriterDashboardResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(underwriterService.getDashboard(user.getUserId()));
    }

    /** Get policy applications assigned to this underwriter that need a quote */
    @GetMapping("/pending-assignments")
    public ResponseEntity<List<PolicyResponse>> getPendingAssignments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(underwriterService.getPendingAssignments(user.getUserId()));
    }

    /** All policies handled by this underwriter (all statuses) */
    @GetMapping("/my-policies")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(underwriterService.getMyPolicies(user.getUserId()));
    }

    /** Available insurance plans */
    @GetMapping("/plans")
    public ResponseEntity<List<InsurancePlanResponse>> getAvailablePlans() {
        return ResponseEntity.ok(underwriterService.getAvailablePlans());
    }

    /** Send a premium quote for a pending policy application */
    @PostMapping("/policy/{policyId}/send-quote")
    public ResponseEntity<PolicyResponse> sendQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @Valid @RequestBody UnderwriterQuoteRequest request) {
        return ResponseEntity.ok(underwriterService.sendPremiumQuote(user.getUserId(), policyId, request));
    }

    /** Auto-calculate premium quote for policy */
    @GetMapping("/policy/{policyId}/calculate-quote")
    public ResponseEntity<UnderwriterQuoteRequest> calculateQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId) {
        java.math.BigDecimal amount = underwriterService.calculateQuoteForPolicy(user.getUserId(), policyId);
        UnderwriterQuoteRequest res = new UnderwriterQuoteRequest();
        res.setQuoteAmount(amount);
        return ResponseEntity.ok(res);
    }
}
