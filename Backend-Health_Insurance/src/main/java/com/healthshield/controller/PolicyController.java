package com.healthshield.controller;

import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.request.PolicyRenewalRequest;
import com.healthshield.dto.response.PolicyMemberResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.User;
import com.healthshield.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchasePolicy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PolicyPurchaseRequest request) {
        return new ResponseEntity<>(
                policyService.purchasePolicy(user.getUserId(), request),
                HttpStatus.CREATED);
    }

    @PostMapping(value = "/with-document", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchasePolicyWithDocument(
            @AuthenticationPrincipal User user,
            @RequestPart("policy") @Valid PolicyPurchaseRequest request,
            @RequestPart(value = "healthCheckReport", required = false) MultipartFile healthCheckReport,
            @RequestPart(value = "aadhaarDocument", required = false) MultipartFile aadhaarDocument) {
        return new ResponseEntity<>(
                policyService.purchasePolicyWithDocument(
                        user.getUserId(), request, healthCheckReport, aadhaarDocument),
                HttpStatus.CREATED);
    }

    @GetMapping("/my-policies")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PolicyResponse>> getMyPolicies(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                policyService.getPoliciesByUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','UNDERWRITER')")
    public ResponseEntity<PolicyResponse> getPolicyById(
            @AuthenticationPrincipal User user,
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
    public ResponseEntity<PolicyResponse> cancelPolicy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(policyService.cancelPolicy(user, id));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','UNDERWRITER')")
    public ResponseEntity<List<PolicyMemberResponse>> getPolicyMembers(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicyMembers(user, id));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> renewPolicy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody PolicyRenewalRequest request) {
        return new ResponseEntity<>(
                policyService.renewPolicy(user, id, request),
                HttpStatus.CREATED);
    }

    /** Download / view health check report */
    @GetMapping("/{id}/document/download")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER','CUSTOMER')")
    public ResponseEntity<Resource> downloadPolicyDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        String docPath = policyService.getPolicyDocumentPath(id);
        return serveFile(docPath);
    }

    /** Download / view Aadhaar document — underwriter + admin only */
    @GetMapping("/{id}/aadhaar/download")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER')")
    public ResponseEntity<Resource> downloadAadhaarDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        String docPath = policyService.getAadhaarDocumentPath(id);
        return serveFile(docPath);
    }

    /** Underwriter verifies KYC */
    @PatchMapping("/{id}/kyc/verify")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<PolicyResponse> verifyKyc(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                policyService.verifyKyc(user.getUserId(), id));
    }

    /** Underwriter rejects KYC */
    @PatchMapping("/{id}/kyc/reject")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public ResponseEntity<PolicyResponse> rejectKyc(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Invalid document");
        return ResponseEntity.ok(
                policyService.rejectKyc(user.getUserId(), id, reason));
    }

    @PutMapping(value = "/{id}/reapply", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> reapplyPolicy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestPart("policy") @Valid PolicyPurchaseRequest request,
            @RequestPart(value = "healthCheckReport", required = false) MultipartFile healthCheckReport,
            @RequestPart(value = "aadhaarDocument", required = false) MultipartFile aadhaarDocument) {
        return ResponseEntity.ok(
                policyService.reapplyPolicy(
                        user.getUserId(), id, request,
                        healthCheckReport, aadhaarDocument));
    }

    // ── FIX 1: Detect real MIME type from file extension ──────────────────
    private ResponseEntity<Resource> serveFile(String docPath) {
        if (docPath == null || docPath.isBlank())
            return ResponseEntity.notFound().build();
        try {
            Path filePath = Paths.get(docPath);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists())
                return ResponseEntity.notFound().build();

            // Detect content type from extension — NOT hardcoded octet-stream
            String filename    = filePath.getFileName().toString().toLowerCase();
            String contentType;

            if      (filename.endsWith(".pdf"))                          contentType = "application/pdf";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.endsWith(".png"))                          contentType = "image/png";
            else if (filename.endsWith(".gif"))                          contentType = "image/gif";
            else if (filename.endsWith(".webp"))                         contentType = "image/webp";
            else {
                // Try OS-level probe as last resort
                String probed = Files.probeContentType(filePath);
                contentType = (probed != null) ? probed : "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filePath.getFileName() + "\"")
                    // Allow Angular to read Content-Type header cross-origin
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Content-Type")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}