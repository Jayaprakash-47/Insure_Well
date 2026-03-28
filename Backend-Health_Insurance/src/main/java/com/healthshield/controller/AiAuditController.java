package com.healthshield.controller;

import com.healthshield.dto.AiAuditResult;
import com.healthshield.entity.Claim;
import com.healthshield.repository.ClaimRepository;
import com.healthshield.service.AiAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class AiAuditController {
    
    private final ClaimRepository claimRepository;
    private final AiAuditService aiAuditService;

    @PostMapping("/{claimId}/ai-audit")
    @PreAuthorize("hasRole('CLAIMS_OFFICER')")
    public ResponseEntity<AiAuditResult> auditClaim(@PathVariable Long claimId) {
        AiAuditResult result = aiAuditService.extractFromPdf(claimId);
        return ResponseEntity.ok(result);
    }
}
