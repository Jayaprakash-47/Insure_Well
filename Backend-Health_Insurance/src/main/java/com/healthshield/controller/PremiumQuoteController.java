package com.healthshield.controller;

import com.healthshield.dto.request.PremiumCalculateRequest;
import com.healthshield.dto.response.PremiumQuoteResponse;
import com.healthshield.entity.User;
import com.healthshield.service.PremiumQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumQuoteController {

    private final PremiumQuoteService premiumQuoteService;

    @PostMapping("/calculate")
    public ResponseEntity<PremiumQuoteResponse> calculateGuest(@Valid @RequestBody PremiumCalculateRequest request) {
        return ResponseEntity.ok(premiumQuoteService.calculatePremium(null, request));
    }

    @PostMapping("/calculate/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PremiumQuoteResponse> calculateLoggedIn(@AuthenticationPrincipal User user,
                                                                   @Valid @RequestBody PremiumCalculateRequest request) {
        return ResponseEntity.ok(premiumQuoteService.calculatePremium(user.getUserId(), request));
    }

    @GetMapping("/my-quotes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PremiumQuoteResponse>> getMyQuotes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(premiumQuoteService.getQuotesByUser(user.getUserId()));
    }
}

