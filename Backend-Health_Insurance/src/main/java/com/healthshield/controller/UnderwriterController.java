package com.healthshield.controller;

import com.healthshield.dto.request.UnderwriterQuoteRequest;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.dto.response.UnderwriterDashboardResponse;
import com.healthshield.entity.Policy;
import com.healthshield.entity.PolicyMember;
import com.healthshield.entity.User;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.service.UnderwriterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final UnderwriterService underwriterService;
    private final PolicyRepository policyRepository; // ← NEW

    // ── Dashboard ──
    @GetMapping("/dashboard")
    public ResponseEntity<UnderwriterDashboardResponse> getDashboard(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getDashboard(user.getUserId()));
    }

    // ── Profile (same as dashboard) ──
    @GetMapping("/profile")
    public ResponseEntity<UnderwriterDashboardResponse> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getDashboard(user.getUserId()));
    }

    // ── All policies assigned to this underwriter (all statuses) ──
    @GetMapping("/policies")
    public ResponseEntity<List<PolicyResponse>> getAllMyPolicies(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getMyPolicies(user.getUserId()));
    }

    // ── My policies (alias) ──
    @GetMapping("/my-policies")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getMyPolicies(user.getUserId()));
    }

    // ── Pending assignments needing a quote ──
    @GetMapping("/pending-assignments")
    public ResponseEntity<List<PolicyResponse>> getPendingAssignments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                underwriterService.getPendingAssignments(user.getUserId()));
    }

    // ── Available insurance plans ──
    @GetMapping("/plans")
    public ResponseEntity<List<InsurancePlanResponse>> getAvailablePlans() {
        return ResponseEntity.ok(underwriterService.getAvailablePlans());
    }

    // ── Send premium quote ──
    @PostMapping("/policy/{policyId}/send-quote")
    public ResponseEntity<PolicyResponse> sendQuote(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @Valid @RequestBody UnderwriterQuoteRequest request) {
        return ResponseEntity.ok(
                underwriterService.sendPremiumQuote(
                        user.getUserId(), policyId, request));
    }

    // ── Auto-calculate quote (legacy — returns UnderwriterQuoteRequest) ──
    @GetMapping("/policy/{policyId}/calculate-quote")
    public ResponseEntity<UnderwriterQuoteRequest> calculateQuoteLegacy(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId) {
        BigDecimal amount = underwriterService
                .calculateQuoteForPolicy(user.getUserId(), policyId);
        UnderwriterQuoteRequest res = new UnderwriterQuoteRequest();
        res.setQuoteAmount(amount);
        return ResponseEntity.ok(res);
    }

    // ── NEW: Calculate quote with full risk breakdown ──
    @GetMapping("/calculate-quote/{policyId}")
    public ResponseEntity<Map<String, Object>> calculateQuoteWithBreakdown(
            @PathVariable Long policyId,
            Authentication auth) {

        Long underwriterId = getUserId(auth);

        // Get calculated quote from service
        BigDecimal calculated = underwriterService
                .calculateQuoteForPolicy(underwriterId, policyId);

        // Build member risk breakdown
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException(
                        "Policy not found: " + policyId));

        List<Map<String, Object>> memberRisks = new ArrayList<>();
        if (policy.getMembers() != null) {
            for (PolicyMember m : policy.getMembers()) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("memberName",  m.getMemberName());
                risk.put("diseases",
                        m.getPreExistingDiseases() != null
                                ? m.getPreExistingDiseases() : "None");
                risk.put("riskLevel",
                        getRiskLevel(m.getPreExistingDiseases()));
                risk.put("diseaseFactor",
                        getDiseaseFactor(m.getPreExistingDiseases()));
                memberRisks.add(risk);
            }
        }

        // Age risk info
        int maxAge = policy.getMembers() != null
                ? policy.getMembers().stream()
                .filter(m -> m.getDateOfBirth() != null)
                .mapToInt(m -> java.time.Period.between(
                        m.getDateOfBirth(),
                        java.time.LocalDate.now()).getYears())
                .max().orElse(30)
                : 30;

        return ResponseEntity.ok(Map.of(
                "calculatedQuote", calculated,
                "memberRisks",     memberRisks,
                "maxAge",          maxAge,
                "ageFactor",       getAgeFactor(maxAge),
                "memberCount",     policy.getMembers() != null
                        ? policy.getMembers().size() : 1
        ));
    }

    // ── Raise concern about a policy ──
    @PostMapping("/policy/{policyId}/raise-concern")
    public ResponseEntity<Map<String, String>> raiseConcern(
            @AuthenticationPrincipal User user,
            @PathVariable Long policyId,
            @RequestBody Map<String, String> request) {
        String remarks = request.getOrDefault("remarks", "");
        underwriterService.raiseConcern(user.getUserId(), policyId, remarks);
        return ResponseEntity.ok(
                Map.of("message", "Concern raised successfully"));
    }

    // ══════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════

    private Long getUserId(Authentication auth) {
        Object principal = auth != null ? auth.getPrincipal() : null;
        if (principal instanceof User user) return user.getUserId();
        throw new IllegalStateException(
                "Authenticated user context is invalid");
    }

    // ── Disease risk level label ──
    private String getRiskLevel(String diseases) {
        if (diseases == null || diseases.trim().isEmpty()
                || diseases.equalsIgnoreCase("none"))
            return "LOW";

        String d = diseases.toLowerCase();

        if (containsAny(d, "cancer", "tumor", "hiv", "aids",
                "organ failure", "transplant", "kidney failure",
                "liver failure", "heart failure"))
            return "CRITICAL";

        if (containsAny(d, "heart", "cardiac", "stroke",
                "paralysis", "parkinson", "alzheimer",
                "multiple sclerosis", "crohn", "lupus",
                "cirrhosis", "chronic kidney"))
            return "HIGH";

        if (containsAny(d, "diabetes", "diabetic",
                "hypertension", "blood pressure", "bp",
                "copd", "asthma", "epilepsy", "seizure",
                "thyroid", "obesity", "arthritis"))
            return "MEDIUM";

        if (containsAny(d, "cholesterol", "fatty liver",
                "gastric", "ulcer", "anxiety", "depression",
                "migraine", "psoriasis", "eczema",
                "back pain", "spine"))
            return "LOW-MEDIUM";

        return "LOW-MEDIUM"; // any other unknown condition
    }

    // ── Disease loading factor ──
    private double getDiseaseFactor(String diseases) {
        if (diseases == null || diseases.trim().isEmpty()
                || diseases.equalsIgnoreCase("none"))
            return 1.0;

        String d = diseases.toLowerCase();

        if (containsAny(d, "cancer", "tumor", "hiv", "aids",
                "organ failure", "transplant", "kidney failure",
                "liver failure", "heart failure"))
            return 1.8;

        if (containsAny(d, "heart", "cardiac", "stroke",
                "paralysis", "parkinson", "alzheimer",
                "multiple sclerosis", "crohn", "lupus",
                "cirrhosis", "chronic kidney"))
            return 1.5;

        if (containsAny(d, "diabetes", "diabetic",
                "hypertension", "blood pressure", "bp",
                "copd", "asthma", "epilepsy", "seizure",
                "thyroid", "obesity", "arthritis"))
            return 1.35;

        if (containsAny(d, "cholesterol", "fatty liver",
                "gastric", "ulcer", "anxiety", "depression",
                "migraine", "psoriasis", "eczema",
                "back pain", "spine"))
            return 1.2;

        return 1.1; // any other condition
    }

    // ── Age loading factor ──
    private double getAgeFactor(int age) {
        if (age <= 30)      return 1.0;
        if (age <= 40)      return 1.2;
        if (age <= 50)      return 1.5;
        if (age <= 60)      return 1.8;
        return 2.2;
    }

    // ── Keyword matcher ──
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}