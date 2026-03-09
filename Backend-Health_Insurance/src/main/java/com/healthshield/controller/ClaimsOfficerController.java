package com.healthshield.controller;

import com.healthshield.dto.request.ClaimReviewRequest;
import com.healthshield.dto.response.ClaimResponse;
import com.healthshield.dto.response.ClaimsOfficerDashboardResponse;
import com.healthshield.entity.User;
import com.healthshield.service.ClaimsOfficerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims-officer")
@PreAuthorize("hasRole('CLAIMS_OFFICER')")
@RequiredArgsConstructor
public class ClaimsOfficerController {

    private final ClaimsOfficerService claimsOfficerService;

    // =================== DASHBOARD ===================

    @GetMapping("/dashboard")
    public ResponseEntity<ClaimsOfficerDashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimsOfficerService.getDashboard(user.getUserId()));
    }

    // =================== CLAIM QUEUE ===================

    /** Get all unassigned claims in the queue waiting for an officer to pick up */
    @GetMapping("/queue")
    public ResponseEntity<List<ClaimResponse>> getUnassignedClaims() {
        return ResponseEntity.ok(claimsOfficerService.getUnassignedClaims());
    }

    /** Get claims assigned to the current officer */
    @GetMapping("/my-claims")
    public ResponseEntity<List<ClaimResponse>> getMyAssignedClaims(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimsOfficerService.getMyAssignedClaims(user.getUserId()));
    }

    /** Get assigned claims filtered by status */
    @GetMapping("/my-claims/status/{status}")
    public ResponseEntity<List<ClaimResponse>> getMyClaimsByStatus(
            @AuthenticationPrincipal User user,
            @PathVariable String status) {
        return ResponseEntity.ok(claimsOfficerService.getMyClaimsByStatus(user.getUserId(), status));
    }

    /** Get history of past decisions made by this officer */
    @GetMapping("/my-decisions")
    public ResponseEntity<List<ClaimResponse>> getMyDecisionHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimsOfficerService.getMyDecisionHistory(user.getUserId()));
    }

    // =================== PICK UP CLAIM ===================

    /** Self-assign a claim from the unassigned queue for review */
    @PostMapping("/claim/{claimId}/pickup")
    public ResponseEntity<ClaimResponse> pickupClaim(
            @AuthenticationPrincipal User user,
            @PathVariable Long claimId) {
        return ResponseEntity.ok(claimsOfficerService.pickupClaim(user.getUserId(), claimId));
    }

    // =================== CLAIM DETAIL ===================

    /** Get full details of a specific claim */
    @GetMapping("/claim/{claimId}")
    public ResponseEntity<ClaimResponse> getClaimDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long claimId) {
        return ResponseEntity.ok(claimsOfficerService.getClaimDetail(user.getUserId(), claimId));
    }

    // =================== REVIEW / DECISION ===================

    /**
     * Submit a review decision on a claim.
     * Supports: APPROVED, PARTIALLY_APPROVED, REJECTED, ESCALATED, DOCUMENT_PENDING
     */
    @PostMapping("/claim/{claimId}/review")
    public ResponseEntity<ClaimResponse> reviewClaim(
            @AuthenticationPrincipal User user,
            @PathVariable Long claimId,
            @Valid @RequestBody ClaimReviewRequest request) {
        return ResponseEntity.ok(claimsOfficerService.reviewClaim(user.getUserId(), claimId, request));
    }
}
