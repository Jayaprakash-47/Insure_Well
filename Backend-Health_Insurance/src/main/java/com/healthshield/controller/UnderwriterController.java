package com.healthshield.controller;

import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.UnderwriterQuoteRequest;
import com.healthshield.dto.response.AgentRequestResponse;
import com.healthshield.dto.response.CustomerSummaryResponse;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.dto.response.UnderwriterDashboardResponse;
import com.healthshield.entity.Policy;
import com.healthshield.entity.User;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.service.AgentRequestService;
import com.healthshield.service.AuditLogService;
import com.healthshield.service.PolicyService;
import com.healthshield.service.UnderwriterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/underwriter")
@PreAuthorize("hasRole('UNDERWRITER')")
@RequiredArgsConstructor
public class UnderwriterController {

    private final UnderwriterService  underwriterService;
    private final AgentRequestService agentRequestService;
    private final PolicyRepository    policyRepository;
    private final PolicyService       policyService;
    private final AuditLogService     auditLogService;
    private final com.healthshield.service.AiDocumentVerificationService aiDocumentVerificationService;

    // ── Dashboard ──────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<UnderwriterDashboardResponse> getDashboard(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getDashboard(user.getUserId()));
    }

    @GetMapping("/profile")
    public ResponseEntity<UnderwriterDashboardResponse> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getDashboard(user.getUserId()));
    }

    // ── Policies ───────────────────────────────────────────────────────────

    @GetMapping("/policies")
    public ResponseEntity<List<PolicyResponse>> getAllMyPolicies(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getMyPolicies(user.getUserId()));
    }

    @GetMapping("/my-policies")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getMyPolicies(user.getUserId()));
    }

    @GetMapping("/pending-assignments")
    public ResponseEntity<List<PolicyResponse>> getPendingAssignments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getPendingAssignments(user.getUserId()));
    }

    // ── Plans ──────────────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<List<InsurancePlanResponse>> getAvailablePlans() {
        return ResponseEntity.ok(underwriterService.getAvailablePlans());
    }

    // ── Calculate Quote ────────────────────────────────────────────────────

    /**
     * GET /api/underwriter/policy/{policyId}/calculate-quote
     *
     * Uses PolicyService.calculateUnderwriterQuote which factors in:
     *   - AI-extracted conditions from health report
     *   - Age of oldest member
     *   - Number of members
     * Returns { quoteAmount: <calculated> }
     */
    @GetMapping("/policy/{policyId}/calculate-quote")
    public ResponseEntity<Map<String, Object>> calculateQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId) {

        BigDecimal amount = underwriterService
                .calculateQuoteForPolicy(user.getUserId(), policyId);

        // Build breakdown for the frontend to display
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException(
                        "Policy not found: " + policyId));

        int maxAge = 30;
        if (policy.getMembers() != null) {
            maxAge = policy.getMembers().stream()
                    .filter(m -> m.getDateOfBirth() != null)
                    .mapToInt(m -> java.time.Period.between(
                            m.getDateOfBirth(),
                            java.time.LocalDate.now()).getYears())
                    .max().orElse(30);
        }

        return ResponseEntity.ok(Map.of(
                "quoteAmount",       amount,
                "maxAge",            maxAge,
                "memberCount",       policy.getMembers() != null
                        ? policy.getMembers().size() : 1,
                "extractedConditions", policy.getExtractedConditions() != null
                        ? policy.getExtractedConditions() : "None"
        ));
    }

    /**
     * Legacy endpoint kept for backward compatibility.
     * GET /api/underwriter/calculate-quote/{policyId}
     * Returns same AI-aware calculation with member risk breakdown.
     */
    @GetMapping("/calculate-quote/{policyId}")
    public ResponseEntity<Map<String, Object>> calculateQuoteWithBreakdown(
            @PathVariable Long policyId,
            Authentication auth) {

        Long underwriterId = getUserId(auth);
        BigDecimal calculated = underwriterService
                .calculateQuoteForPolicy(underwriterId, policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException(
                        "Policy not found: " + policyId));

        // Build per-member risk summary for display
        List<Map<String, Object>> memberRisks = new ArrayList<>();
        if (policy.getMembers() != null) {
            for (var m : policy.getMembers()) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("memberName", m.getMemberName());
                risk.put("diseases",
                        m.getPreExistingDiseases() != null
                                ? m.getPreExistingDiseases() : "None");
                risk.put("riskLevel",
                        getRiskLevel(m.getPreExistingDiseases()));
                memberRisks.add(risk);
            }
        }

        int maxAge = 30;
        if (policy.getMembers() != null) {
            maxAge = policy.getMembers().stream()
                    .filter(m -> m.getDateOfBirth() != null)
                    .mapToInt(m -> java.time.Period.between(
                            m.getDateOfBirth(),
                            java.time.LocalDate.now()).getYears())
                    .max().orElse(30);
        }

        return ResponseEntity.ok(Map.of(
                "quoteAmount",         calculated,
                "memberRisks",         memberRisks,
                "maxAge",              maxAge,
                "memberCount",         policy.getMembers() != null
                        ? policy.getMembers().size() : 1,
                "extractedConditions", policy.getExtractedConditions() != null
                        ? policy.getExtractedConditions() : "None"
        ));
    }

    // ── Send Quote ─────────────────────────────────────────────────────────

    @PostMapping("/policy/{policyId}/send-quote")
    public ResponseEntity<PolicyResponse> sendQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @Valid @RequestBody UnderwriterQuoteRequest request) {
        return ResponseEntity.ok(
                underwriterService.sendPremiumQuote(
                        user.getUserId(), policyId, request));
    }

    // ── KYC ────────────────────────────────────────────────────────────────

    @PutMapping("/policy/{policyId}/verify-kyc")
    public ResponseEntity<PolicyResponse> verifyKyc(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId) {
        return ResponseEntity.ok(
                policyService.verifyKyc(user.getUserId(), policyId));
    }

    @PutMapping("/policy/{policyId}/reject-kyc")
    public ResponseEntity<PolicyResponse> rejectKyc(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "KYC document unclear");
        return ResponseEntity.ok(
                policyService.rejectKyc(user.getUserId(), policyId, reason));
    }

    // ── AI Document Verification ───────────────────────────────────────────

    @PostMapping("/policy/{policyId}/ai-verify")
    public ResponseEntity<Map<String, String>> aiVerifyDocuments(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId) {
        try {
            String report = aiDocumentVerificationService.verifyDocuments(policyId);
            return ResponseEntity.ok(Map.of(
                    "message", "Verification complete",
                    "report", report
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "AI Verification failed: " + e.getMessage()));
        }
    }

    // ── Raise Concern ──────────────────────────────────────────────────────

    @PostMapping("/policy/{policyId}/raise-concern")
    public ResponseEntity<Map<String, String>> raiseConcern(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @RequestBody Map<String, String> request) {
        underwriterService.raiseConcern(
                user.getUserId(), policyId,
                request.getOrDefault("remarks", ""));
        return ResponseEntity.ok(Map.of("message", "Concern raised successfully"));
    }

    // ── Customers ──────────────────────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerSummaryResponse>> getAllCustomers() {
        return ResponseEntity.ok(underwriterService.getAllCustomers());
    }

    // ── Agent Requests ─────────────────────────────────────────────────────

    @GetMapping("/agent-requests/pending")
    public ResponseEntity<List<AgentRequestResponse>> getPendingAgentRequests() {
        return ResponseEntity.ok(agentRequestService.getPendingRequests());
    }

    @GetMapping("/agent-requests/my-accepted")
    public ResponseEntity<List<AgentRequestResponse>> getMyAcceptedRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                agentRequestService.getMyAcceptedRequests(user.getUserId()));
    }

    @PutMapping("/agent-requests/{id}/accept")
    public ResponseEntity<AgentRequestResponse> acceptRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                agentRequestService.acceptRequest(user.getUserId(), id));
    }

    @PostMapping("/agent-requests/{id}/apply")
    public ResponseEntity<PolicyResponse> applyForCustomer(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody PolicyPurchaseRequest request) {
        return new ResponseEntity<>(
                agentRequestService.applyForCustomer(user.getUserId(), id, request),
                HttpStatus.CREATED);
    }

    // ── Direct Apply ───────────────────────────────────────────────────────

    @PostMapping("/apply-direct/{customerId}")
    public ResponseEntity<PolicyResponse> applyDirect(
            @AuthenticationPrincipal User user,
            @PathVariable Long customerId,
            @Valid @RequestBody PolicyPurchaseRequest request) {
        PolicyResponse policy = policyService.purchasePolicy(customerId, request);
        auditLogService.log("POLICY_APPLIED_BY_AGENT", "UNDERWRITER",
                user.getEmail(),
                "Direct assisted application: policy " + policy.getPolicyNumber()
                        + " applied for customer ID " + customerId);
        return new ResponseEntity<>(policy, HttpStatus.CREATED);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof User user)
            return user.getUserId();
        throw new IllegalStateException("Authenticated user context is invalid");
    }

    /** Display-only risk label used in the quote breakdown response */
    private String getRiskLevel(String diseases) {
        if (diseases == null || diseases.trim().isEmpty()
                || diseases.equalsIgnoreCase("none")) return "LOW";
        String d = diseases.toLowerCase();
        if (containsAny(d, "cancer", "tumor", "hiv", "aids",
                "organ failure", "transplant", "kidney failure",
                "liver failure", "heart failure"))  return "CRITICAL";
        if (containsAny(d, "heart", "cardiac", "stroke",
                "paralysis", "parkinson", "alzheimer")) return "HIGH";
        if (containsAny(d, "diabetes", "diabetic", "hypertension",
                "blood pressure", "asthma", "epilepsy",
                "thyroid", "obesity"))               return "MEDIUM";
        return "LOW-MEDIUM";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords)
            if (text.contains(k)) return true;
        return false;
    }
}