package com.healthshield.controller;

import com.healthshield.entity.Payment;
import com.healthshield.entity.Policy;
import com.healthshield.enums.NotificationType;
import com.healthshield.enums.PaymentMethod;
import com.healthshield.enums.PaymentStatus;
import com.healthshield.repository.PaymentRepository;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.repository.UserRepository;
import com.healthshield.service.AuditLogService;
import com.healthshield.service.NotificationService;
import com.healthshield.service.PolicyService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")   // ← different URL to avoid conflict
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://localhost:62486"
})
public class RazorpayController {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private final PolicyRepository policyRepository;
    private final PolicyService policyService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long policyId = Long.parseLong(body.get("policyId").toString());
            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            if (!policy.getUser().getEmail().equals(auth.getName())) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Unauthorized"));
            }

            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            int amountInPaise = policy.getPremiumAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .intValue();
            options.put("amount", amountInPaise);
            options.put("currency", "INR");
            options.put("receipt", "policy_" + policy.getPolicyNumber());

            Order order = client.orders.create(options);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId",       order.get("id"));
            response.put("amount",        amountInPaise);
            response.put("currency",      "INR");
            response.put("keyId",         keyId);
            response.put("policyId",      policyId);
            response.put("policyNumber",  policy.getPolicyNumber());
            response.put("planName",      policy.getPlan().getPlanName());
            response.put("customerName",
                    policy.getUser().getFirstName()
                            + " " + policy.getUser().getLastName());
            response.put("customerEmail", policy.getUser().getEmail());
            response.put("customerPhone",
                    policy.getUser().getPhone() != null
                            ? policy.getUser().getPhone() : "");

            log.info("Razorpay order created for policy {}",
                    policy.getPolicyNumber());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String razorpayOrderId   = body.get("razorpayOrderId");
            String razorpayPaymentId = body.get("razorpayPaymentId");
            String razorpaySignature = body.get("razorpaySignature");
            Long policyId = Long.parseLong(body.get("policyId"));

            // Verify HMAC SHA256 signature
            String data = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    keySecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
            String generated = new String(Hex.encodeHex(hash));

            if (!generated.equals(razorpaySignature)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid payment signature"));
            }

            Policy policy = policyRepository.findById(policyId)
                    .orElseThrow(() -> new RuntimeException("Policy not found"));

            // ── NEW: Save payment record to DB ──

            Payment payment = Payment.builder()
                    .policy(policy)
                    .user(policy.getUser())
                    .amount(policy.getPremiumAmount())
                    .paymentMethod(PaymentMethod.RAZORPAY)
                    .transactionId(razorpayPaymentId)
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .build();
            paymentRepository.save(payment);
            log.info("Payment saved: {} for policy {}",
                    razorpayPaymentId, policy.getPolicyNumber());

            // Activate policy — sends welcome email + notification
            policyService.activatePolicy(policy);

            // ── NEW: Additional payment success notification ──
            notificationService.sendNotification(
                    policy.getUser().getEmail(),
                    "💳 Payment of ₹" + policy.getPremiumAmount()
                            + " received for policy "
                            + policy.getPolicyNumber()
                            + ". Transaction ID: " + razorpayPaymentId,
                    NotificationType.POLICY_APPROVED
            );

            auditLogService.log("PAYMENT_SUCCESS", "CUSTOMER",
                    policy.getUser().getEmail(),
                    "Razorpay payment " + razorpayPaymentId
                            + " verified. Amount: ₹"
                            + policy.getPremiumAmount()
                            + " for policy " + policy.getPolicyNumber());

            return ResponseEntity.ok(Map.of(
                    "message",   "Payment verified! Policy is now ACTIVE.",
                    "paymentId", razorpayPaymentId,
                    "policy",    policy.getPolicyNumber()
            ));

        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/failure")
    public ResponseEntity<?> handleFailure(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        log.warn("Payment failed for policy {}: {}",
                body.get("policyId"), body.get("errorDescription"));
        auditLogService.log("PAYMENT_FAILED", "CUSTOMER", auth.getName(),
                "Payment failed for policy " + body.get("policyId")
                        + ". Error: " + body.get("errorDescription"));
        return ResponseEntity.ok(Map.of("message", "Failure recorded"));
    }
}