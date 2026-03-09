package com.healthshield.controller;

import com.healthshield.dto.request.PaymentRequest;
import com.healthshield.dto.response.PaymentResponse;
import com.healthshield.entity.User;
import com.healthshield.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> makePayment(@AuthenticationPrincipal User user,
                                                        @Valid @RequestBody PaymentRequest request) {
        return new ResponseEntity<>(paymentService.processPayment(user.getUserId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(user.getUserId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<PaymentResponse> getPaymentById(@AuthenticationPrincipal User user,
                                                            @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(user, id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/policy/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getByPolicy(@AuthenticationPrincipal User user,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentsByPolicy(user, id));
    }
}

