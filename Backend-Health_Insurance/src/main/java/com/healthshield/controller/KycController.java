package com.healthshield.controller;

import com.healthshield.entity.Customer;
import com.healthshield.entity.Policy;
import com.healthshield.entity.User;
import com.healthshield.enums.KycStatus;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.repository.UserRepository;
import com.healthshield.service.AuditLogService;
import com.healthshield.service.N8nEmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Self-hosted Aadhaar e-KYC simulation.
 *
 * Since real Aadhaar OTP APIs (sandbox.co.in) require paid credits, this controller
 * replicates the exact same UX flow using the customer's registered email as the
 * OTP delivery channel:
 *
 *   1. POST /api/kyc/initiate   — validates Aadhaar format, generates a 6-digit OTP,
 *                                  emails it to the customer's registered email via n8n.
 *   2. POST /api/kyc/verify-otp — validates OTP (10-min TTL), marks Aadhaar as verified.
 *
 * The frontend API contract is identical to the Sandbox version — no Angular changes needed.
 */
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:62486"})
public class KycController {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final N8nEmailService emailService;
    private final AuditLogService auditLogService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    /**
     * In-memory OTP session store.
     * Key: transactionId (UUID)
     * Value: OtpSession (holds OTP, aadhaar, email, expiry)
     */
    private static final Map<String, OtpSession> sessionStore = new ConcurrentHashMap<>();

    // ── Step 1: Initiate KYC — generate & email OTP ────────────────────────────
    @PostMapping("/initiate")
    public ResponseEntity<?> initiateKyc(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Accept both field name styles from frontend
        String aadhaar = extractString(body, "aadhaar_number", "aadhaarNumber");

        if (aadhaar == null || !aadhaar.matches("\\d{12}")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid Aadhaar number. Must be exactly 12 digits."));
        }

        // Load the authenticated customer
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "User not found. Please log in again."));
        }

        // Generate a 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        String transactionId = UUID.randomUUID().toString();

        // Store session with TTL
        sessionStore.put(transactionId, new OtpSession(otp, aadhaar, email, System.currentTimeMillis()));

        // Send OTP via n8n email webhook
        try {
            String maskedEmail = maskEmail(email);
            emailService.sendEmail(
                    email,
                    "InsureWell — Aadhaar e-KYC OTP",
                    buildKycOtpHtml(user.getFirstName(), otp, aadhaar)
            );
            log.info("[KYC] OTP sent to {} for Aadhaar ending ...{}", email, aadhaar.substring(8));

            return ResponseEntity.ok(Map.of(
                    "transactionId", transactionId,
                    "message", "OTP sent to your registered email (" + maskedEmail + ")"
            ));

        } catch (Exception e) {
            log.error("[KYC] Failed to send OTP email: {}", e.getMessage());
            sessionStore.remove(transactionId);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to send OTP. Please try again."));
        }
    }

    // ── Step 2: Verify OTP ────────────────────────────────────────────────────
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {

        OtpSession session = sessionStore.get(req.getTransactionId());

        if (session == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Session expired or invalid. Please restart KYC."));
        }

        // TTL check
        if (System.currentTimeMillis() - session.getCreatedAt() > OTP_TTL_MS) {
            sessionStore.remove(req.getTransactionId());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "OTP has expired. Please request a new one."));
        }

        // OTP match
        if (!session.getOtp().equals(req.getOtp().trim())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Incorrect OTP. Please check your email and try again."));
        }

        // OTP correct — update user record
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user instanceof Customer customer) {
            customer.setAadhaarNumber(session.getAadhaarNumber());
            customer.setAadhaarVerified(true);
            userRepository.save(customer);

            // Auto-verify KYC on all PENDING policies for this customer
            List<Policy> pendingPolicies = policyRepository.findByUserUserId(customer.getUserId());
            for (Policy policy : pendingPolicies) {
                if (policy.getKycStatus() == KycStatus.PENDING) {
                    policy.setKycStatus(KycStatus.VERIFIED);
                    policyRepository.save(policy);
                    log.info("[KYC] Auto-verified KYC for policy {}", policy.getPolicyNumber());
                }
            }

            auditLogService.log(
                    "KYC_VERIFIED",
                    "CUSTOMER",
                    email,
                    "Aadhaar e-KYC verified for customer " + email
                            + " (last 4: " + session.getAadhaarNumber().substring(8) + ")"
            );
        }

        sessionStore.remove(req.getTransactionId());
        log.info("[KYC] Aadhaar KYC verified for {}", email);

        return ResponseEntity.ok(Map.of(
                "verified", true,
                "transactionId", req.getTransactionId(),
                "name", user != null ? user.getFirstName() + " " + user.getLastName() : "Verified",
                "message", "Aadhaar e-KYC Verified Successfully"
        ));
    }

    // ── Step 3: Check session status ─────────────────────────────────────────
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getStatus(@PathVariable String transactionId) {
        OtpSession session = sessionStore.get(transactionId);
        boolean expired = session == null
                || (System.currentTimeMillis() - session.getCreatedAt() > OTP_TTL_MS);
        return ResponseEntity.ok(Map.of(
                "verified", (session == null && !expired),
                "active", !expired && session != null,
                "transactionId", transactionId
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractString(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object val = body.get(key);
            if (val != null) return val.toString().trim();
        }
        return null;
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return email;
        return email.charAt(0) + "***" + email.substring(at - 1);
    }

    private String buildKycOtpHtml(String name, String otp, String aadhaar) {
        String maskedAadhaar = "XXXX XXXX " + aadhaar.substring(8);
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;
                        border:1px solid #e5e7eb;border-radius:12px;overflow:hidden">
              <div style="background:linear-gradient(135deg,#800020,#9b1c3a);
                          padding:28px;text-align:center">
                <h2 style="color:white;margin:0;font-size:22px">InsureWell Insurance</h2>
                <p style="color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:14px">
                  Aadhaar e-KYC Verification
                </p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px">Dear <strong>%s</strong>,</p>
                <p style="color:#4b5563">
                  We received a request to verify your Aadhaar identity for your
                  InsureWell policy application. Use the OTP below to complete verification.
                </p>
                <div style="text-align:center;margin:28px 0">
                  <p style="color:#6b7280;font-size:13px;margin-bottom:8px">
                    Aadhaar: <strong>%s</strong>
                  </p>
                  <div style="font-size:42px;font-weight:bold;letter-spacing:16px;
                              color:#800020;margin:16px 0;padding:20px 24px;
                              background:#fef2f2;border-radius:10px;
                              border:2px dashed #fca5a5;display:inline-block">
                    %s
                  </div>
                  <p style="color:#6b7280;font-size:13px;margin-top:12px">
                    This OTP expires in <strong>10 minutes</strong>.
                  </p>
                </div>
                <div style="background:#fffbeb;border:1px solid #fde68a;
                             border-radius:8px;padding:14px;font-size:13px;color:#92400e">
                  <strong>Security Notice:</strong> InsureWell will never ask you to share
                  this OTP with anyone. If you did not request this, please ignore this email.
                </div>
              </div>
              <div style="background:#f9fafb;padding:12px;text-align:center;
                          font-size:12px;color:#9ca3af">
                InsureWell Insurance — Automated KYC Notification
              </div>
            </div>
            """.formatted(name != null ? name : "Customer", maskedAadhaar, otp);
    }

    // ── DTOs & Session model ──────────────────────────────────────────────────

    @Data
    public static class VerifyOtpRequest {
        private String transactionId;
        private String otp;
    }

    @Data
    static class OtpSession {
        private final String otp;
        private final String aadhaarNumber;
        private final String email;
        private final long createdAt;
    }
}
