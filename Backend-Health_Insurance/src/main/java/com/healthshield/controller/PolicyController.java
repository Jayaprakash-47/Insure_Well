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
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @PostMapping(value = "/with-document", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchasePolicyWithDocument(@AuthenticationPrincipal User user,
                                                                       @RequestPart("policy") @Valid PolicyPurchaseRequest request,
                                                                       @RequestPart(value = "healthCheckReport", required = false) MultipartFile healthCheckReport) {
        return new ResponseEntity<>(policyService.purchasePolicyWithDocument(user.getUserId(), request, healthCheckReport), HttpStatus.CREATED);
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

    /** Download health check report document for a policy */
    @GetMapping("/{id}/document/download")
    @PreAuthorize("hasAnyRole('ADMIN','UNDERWRITER','CUSTOMER')")
    public ResponseEntity<Resource> downloadPolicyDocument(@AuthenticationPrincipal User user,
                                                            @PathVariable Long id) {
        String docPath = policyService.getPolicyDocumentPath(id);
        if (docPath == null || docPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = Paths.get(docPath);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
