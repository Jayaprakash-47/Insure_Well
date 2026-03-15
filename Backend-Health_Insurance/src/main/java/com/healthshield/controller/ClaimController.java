package com.healthshield.controller;

import com.healthshield.dto.request.ClaimRequest;
import com.healthshield.dto.request.ClaimStatusUpdateRequest;
import com.healthshield.dto.response.ClaimDocumentResponse;
import com.healthshield.dto.response.ClaimResponse;
import com.healthshield.entity.Claim;
import com.healthshield.entity.User;
import com.healthshield.repository.ClaimRepository;
import com.healthshield.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimRepository claimRepository;

    // ── File a new claim ──
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> fileClaim(
            @AuthenticationPrincipal User user,
            @RequestPart("claim") @Valid ClaimRequest request,
            @RequestPart(value = "documents", required = false)
            List<MultipartFile> documents) {
        return new ResponseEntity<>(
                claimService.fileClaim(user.getUserId(), request, documents),
                HttpStatus.CREATED);
    }

    // ── Customer: my claims ──
    @GetMapping("/my-claims")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ClaimResponse>> getMyClaims(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                claimService.getClaimsByUser(user.getUserId()));
    }

    // ── Get single claim by ID ──
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','CLAIMS_OFFICER')")
    public ResponseEntity<ClaimResponse> getClaimById(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimById(user, id));
    }

    // ── Get all claims (Claims Officer) ──
    @GetMapping
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER')")
    public ResponseEntity<List<ClaimResponse>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    // ── Get pending claims ──
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','CLAIMS_OFFICER')")
    public ResponseEntity<List<ClaimResponse>> getPendingClaims() {
        return ResponseEntity.ok(claimService.getPendingClaims());
    }

    // ── Get claims by status ──
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','CLAIMS_OFFICER')")
    public ResponseEntity<List<ClaimResponse>> getClaimsByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(claimService.getClaimsByStatus(status));
    }

    // ── Admin: update claim status ──
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClaimResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody ClaimStatusUpdateRequest request) {
        return ResponseEntity.ok(claimService.updateClaimStatus(id, request));
    }

    // ── Get claim documents ──
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','CLAIMS_OFFICER','UNDERWRITER')")
    public ResponseEntity<List<ClaimDocumentResponse>> getDocuments(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaimDocuments(user, id));
    }

    // ── Download by docId ──
    @GetMapping("/{claimId}/documents/{docId}/download")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','CLAIMS_OFFICER','UNDERWRITER')")
    public ResponseEntity<Resource> downloadDocumentById(
            @AuthenticationPrincipal User user,
            @PathVariable Long claimId,
            @PathVariable Long docId) {
        List<ClaimDocumentResponse> docs =
                claimService.getClaimDocuments(user, claimId);
        ClaimDocumentResponse doc = docs.stream()
                .filter(d -> d.getDocumentId().equals(docId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Document not found"));
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + doc.getFileName() + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── View by fileName (opens inline) ──
    @GetMapping("/{claimId}/documents/view/{fileName}")
    public ResponseEntity<Resource> viewDocumentByName(
            @PathVariable Long claimId,
            @PathVariable String fileName) {
        try {
            Claim claim = claimRepository.findById(claimId)
                    .orElseThrow(() -> new RuntimeException(
                            "Claim not found"));

            Path filePath = Paths.get("uploads/claims",
                    claim.getClaimNumber(), fileName);
            log.info("View request — looking for: {}",
                    filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("File not found: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error viewing document: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Download by fileName (saves file) ──
    @GetMapping("/{claimId}/documents/download/{fileName}")
    public ResponseEntity<Resource> downloadDocumentByName(
            @PathVariable Long claimId,
            @PathVariable String fileName) {
        try {
            Claim claim = claimRepository.findById(claimId)
                    .orElseThrow(() -> new RuntimeException(
                            "Claim not found"));

            Path filePath = Paths.get("uploads/claims",
                    claim.getClaimNumber(), fileName);
            log.info("Download request — looking for: {}",
                    filePath.toAbsolutePath());

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                log.error("File not found: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading document: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Settle claim (Claims Officer + Admin) ──
    // ← FIXED: Only ONE settle endpoint (removed duplicate)
    @PostMapping("/{claimId}/settle")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'ADMIN')")
    public ResponseEntity<ClaimResponse> settleClaim(
            @PathVariable Long claimId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(claimService.settleClaim(claimId, user));
    }
}