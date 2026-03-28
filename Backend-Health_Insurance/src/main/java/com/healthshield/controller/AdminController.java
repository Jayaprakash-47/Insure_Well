package com.healthshield.controller;

import com.healthshield.dto.request.AdminAssignUnderwriterRequest;
import com.healthshield.dto.request.AdminAssignClaimsOfficerRequest;
import com.healthshield.dto.request.AdminClaimDecisionRequest;
import com.healthshield.dto.request.CreateUnderwriterRequest;
import com.healthshield.dto.request.CreateClaimsOfficerRequest;
import com.healthshield.dto.request.InsurancePlanRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.dto.response.DashboardResponse;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.entity.User;
import com.healthshield.enums.NotificationType;
import com.healthshield.service.AdminService;
import com.healthshield.service.ClaimService;
import com.healthshield.service.InsurancePlanService;
import com.healthshield.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService         adminService;
    private final InsurancePlanService insurancePlanService;
    private final ClaimService         claimService;
    // FIX 5: Inject NotificationService to send bell notification to claims officer
    private final NotificationService  notificationService;

    // =================== DASHBOARD ===================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // =================== USER MANAGEMENT ===================

    @PostMapping("/create-underwriter")
    public ResponseEntity<AuthResponse> createUnderwriter(
            @Valid @RequestBody CreateUnderwriterRequest request) {
        return new ResponseEntity<>(
                adminService.createUnderwriter(request), HttpStatus.CREATED);
    }

    @PostMapping("/create-claims-officer")
    public ResponseEntity<AuthResponse> createClaimsOfficer(
            @Valid @RequestBody CreateClaimsOfficerRequest request) {
        return new ResponseEntity<>(
                adminService.createClaimsOfficer(request), HttpStatus.CREATED);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    @GetMapping("/underwriters")
    public ResponseEntity<List<Map<String, Object>>> getAllUnderwriters() {
        return ResponseEntity.ok(adminService.getAllUnderwriters());
    }

    @GetMapping("/claims-officers")
    public ResponseEntity<List<Map<String, Object>>> getAllClaimsOfficers() {
        return ResponseEntity.ok(adminService.getAllClaimsOfficers());
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateUser(@PathVariable Long id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        adminService.activateUser(id);
        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    // =================== ASSIGN UNDERWRITER TO POLICY ===================

    @PostMapping("/policies/{policyId}/assign-underwriter")
    public ResponseEntity<Map<String, Object>> assignUnderwriter(
            @PathVariable Long policyId,
            @Valid @RequestBody AdminAssignUnderwriterRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(
                adminService.assignUnderwriter(policyId, request, admin));
    }

    @GetMapping("/pending-applications")
    public ResponseEntity<List<Map<String, Object>>> getPendingApplications() {
        return ResponseEntity.ok(adminService.getPendingPolicyApplications());
    }

    // =================== ASSIGN CLAIMS OFFICER TO CLAIM ===================

    /**
     * FIX 5: After assigning the claims officer, send a real-time SSE
     * notification so the bell icon updates immediately on their dashboard.
     */
    @PostMapping("/claims/{claimId}/assign-officer")
    public ResponseEntity<Map<String, Object>> assignClaimsOfficer(
            @PathVariable Long claimId,
            @Valid @RequestBody AdminAssignClaimsOfficerRequest request,
            @AuthenticationPrincipal User admin) {

        Map<String, Object> result =
                adminService.assignClaimsOfficer(claimId, request, admin);

        // FIX 5: Send notification bell to the assigned claims officer
        try {
            String officerEmail = (String) result.get("officerEmail");
            String claimNumber  = (String) result.get("claimNumber");
            String customerName = (String) result.get("customerName");

            if (officerEmail != null) {
                String message = "📋 New claim assigned to you" +
                        (claimNumber  != null ? ": " + claimNumber  : "") +
                        (customerName != null ? " from " + customerName : "") +
                        ". Please log in to begin your review.";

                notificationService.sendNotification(
                        officerEmail,
                        message,
                        NotificationType.CLAIM_SUBMITTED
                );
                log.info("Notification sent to claims officer {} for claim {}",
                        officerEmail, claimNumber);
            }
        } catch (Exception e) {
            // Don't fail the assignment if notification fails
            log.warn("Could not send notification to claims officer: {}",
                    e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/submitted-claims")
    public ResponseEntity<List<Map<String, Object>>> getSubmittedClaims() {
        return ResponseEntity.ok(adminService.getSubmittedClaims());
    }

    // =================== PLAN MANAGEMENT ===================

    @PostMapping("/plans")
    public ResponseEntity<InsurancePlanResponse> createPlan(
            @Valid @RequestBody InsurancePlanRequest request) {
        return new ResponseEntity<>(
                insurancePlanService.createPlan(request), HttpStatus.CREATED);
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<InsurancePlanResponse> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody InsurancePlanRequest request) {
        return ResponseEntity.ok(insurancePlanService.updatePlan(id, request));
    }

    @PatchMapping("/plans/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivatePlan(@PathVariable Long id) {
        insurancePlanService.deactivatePlan(id);
        return ResponseEntity.ok(Map.of("message", "Plan deactivated successfully"));
    }

    @PatchMapping("/plans/{id}/activate")
    public ResponseEntity<Map<String, String>> activatePlan(@PathVariable Long id) {
        insurancePlanService.activatePlan(id);
        return ResponseEntity.ok(Map.of("message", "Plan activated successfully"));
    }
}