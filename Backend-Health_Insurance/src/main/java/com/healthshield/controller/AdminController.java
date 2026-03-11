package com.healthshield.controller;

import com.healthshield.dto.request.AdminAssignUnderwriterRequest;
import com.healthshield.dto.request.AdminAssignClaimsOfficerRequest;
import com.healthshield.dto.request.AdminClaimDecisionRequest;
import com.healthshield.dto.request.CreateUnderwriterRequest;
import com.healthshield.dto.request.CreateClaimsOfficerRequest;
import com.healthshield.dto.request.InsurancePlanRequest;
//import com.healthshield.dto.response.AuditLogResponse;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.dto.response.DashboardResponse;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.entity.User;
import com.healthshield.service.AdminService;
//import com.healthshield.service.AuditService;
import com.healthshield.service.ClaimService;
import com.healthshield.service.InsurancePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class AdminController {

    private final AdminService adminService;
    private final InsurancePlanService insurancePlanService;
    private final ClaimService claimService;
//    private final AuditService auditService;

    // =================== DASHBOARD ===================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // =================== USER MANAGEMENT ===================

    @PostMapping("/create-underwriter")
    public ResponseEntity<AuthResponse> createUnderwriter(@Valid @RequestBody CreateUnderwriterRequest request) {
        return new ResponseEntity<>(adminService.createUnderwriter(request), HttpStatus.CREATED);
    }

    @PostMapping("/create-claims-officer")
    public ResponseEntity<AuthResponse> createClaimsOfficer(@Valid @RequestBody CreateClaimsOfficerRequest request) {
        return new ResponseEntity<>(adminService.createClaimsOfficer(request), HttpStatus.CREATED);
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

    /** Admin assigns a specific underwriter to a PENDING policy application */
    @PostMapping("/policies/{policyId}/assign-underwriter")
    public ResponseEntity<Map<String, Object>> assignUnderwriter(
            @PathVariable Long policyId,
            @Valid @RequestBody AdminAssignUnderwriterRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.assignUnderwriter(policyId, request, admin));
    }

    /** Admin views all PENDING policy applications (awaiting underwriter assignment) */
    @GetMapping("/pending-applications")
    public ResponseEntity<List<Map<String, Object>>> getPendingApplications() {
        return ResponseEntity.ok(adminService.getPendingPolicyApplications());
    }

    // =================== ASSIGN CLAIMS OFFICER TO CLAIM ===================

    /** Admin assigns a claims officer to a SUBMITTED (unassigned) claim */
    @PostMapping("/claims/{claimId}/assign-officer")
    public ResponseEntity<Map<String, Object>> assignClaimsOfficer(
            @PathVariable Long claimId,
            @Valid @RequestBody AdminAssignClaimsOfficerRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.assignClaimsOfficer(claimId, request, admin));
    }

    /** Admin views all SUBMITTED claims (not yet assigned to any officer) */
    @GetMapping("/submitted-claims")
    public ResponseEntity<List<Map<String, Object>>> getSubmittedClaims() {
        return ResponseEntity.ok(adminService.getSubmittedClaims());
    }

    // =================== PLAN MANAGEMENT ===================

    @PostMapping("/plans")
    public ResponseEntity<InsurancePlanResponse> createPlan(@Valid @RequestBody InsurancePlanRequest request) {
        return new ResponseEntity<>(insurancePlanService.createPlan(request), HttpStatus.CREATED);
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<InsurancePlanResponse> updatePlan(@PathVariable Long id,
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

    // =================== ESCALATED CLAIMS - COMMENTED OUT ===================
    /*
    @GetMapping("/escalated-claims")
    public ResponseEntity<List<Map<String, Object>>> getEscalatedClaims() {
        return ResponseEntity.ok(adminService.getEscalatedClaims());
    }

    @PostMapping("/escalated-claims/{claimId}/resolve")
    public ResponseEntity<Map<String, Object>> resolveEscalatedClaim(
            @PathVariable Long claimId,
            @Valid @RequestBody AdminClaimDecisionRequest request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.resolveEscalatedClaim(claimId, request, admin));
    }
    */

    // =================== CLAIM SETTLEMENT ===================

//    @PostMapping("/claims/{claimId}/settle")
//    public ResponseEntity<?> settleClaim(@PathVariable Long claimId, @AuthenticationPrincipal User admin) {
//        return ResponseEntity.ok(claimService.settleClaim(claimId, admin));
//    }

    // =================== UNDERWRITER PERFORMANCE ===================

//    @GetMapping("/underwriter-performance")
//    public ResponseEntity<List<Map<String, Object>>> getUnderwriterPerformance() {
//        return ResponseEntity.ok(adminService.getUnderwriterPerformance());
//    }

    // =================== AUDIT TRAIL ===================

//    @GetMapping("/audit-logs")
//    public ResponseEntity<List<AuditLogResponse>> getRecentAuditLogs() {
//        return ResponseEntity.ok(auditService.getRecentAuditLogs());
//    }
//
//    @GetMapping("/audit-logs/entity/{entityType}/{entityId}")
//    public ResponseEntity<List<AuditLogResponse>> getAuditTrail(
//            @PathVariable String entityType,
//            @PathVariable Long entityId) {
//        return ResponseEntity.ok(auditService.getAuditTrail(entityType.toUpperCase(), entityId));
//    }
//
//    @GetMapping("/audit-logs/type/{entityType}")
//    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByType(@PathVariable String entityType) {
//        return ResponseEntity.ok(auditService.getAuditLogsByType(entityType.toUpperCase()));
//    }
}
