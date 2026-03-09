package com.healthshield.service;

import com.healthshield.dto.request.PaymentRequest;
import com.healthshield.dto.response.PaymentResponse;
import com.healthshield.entity.Payment;
import com.healthshield.entity.Policy;
import com.healthshield.entity.User;
import com.healthshield.enums.PaymentMethod;
import com.healthshield.enums.PaymentStatus;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.PaymentRepository;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final UserRepository userRepository;
    private final PolicyService policyService;

    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + request.getPolicyId()));

        // Validate policy belongs to the user
        if (!policy.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("This policy does not belong to you");
        }

        // Validate policy is in PENDING status (not already paid or cancelled)
        if (policy.getPolicyStatus() != PolicyStatus.PENDING) {
            throw new BadRequestException("Payment can only be made for PENDING policies. Current status: " + policy.getPolicyStatus());
        }

        // Validate payment amount matches premium
        if (request.getAmount().compareTo(policy.getPremiumAmount()) != 0) {
            throw new BadRequestException("Payment amount (₹" + request.getAmount() +
                    ") does not match policy premium (₹" + policy.getPremiumAmount() + ")");
        }

        String transactionId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .policy(policy)
                .user(user)
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .transactionId(transactionId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        Payment saved = paymentRepository.save(payment);

        // Activate policy on successful payment
        policyService.activatePolicy(policy);

        return mapToResponse(saved, "Payment processed successfully");
    }

    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserUserId(userId).stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    public PaymentResponse getPaymentById(User currentUser, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (!isOwnerOrAdmin(currentUser, payment)) {
            throw new UnauthorizedException("You are not authorized to view this payment");
        }

        return mapToResponse(payment, null);
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByPolicy(User currentUser, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !policy.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You are not authorized to view payments for this policy");
        }

        return paymentRepository.findByPolicyPolicyId(policyId).stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    private boolean isOwnerOrAdmin(User user, Payment payment) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin || payment.getUser().getUserId().equals(user.getUserId());
    }

    private PaymentResponse mapToResponse(Payment payment, String message) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .policyId(payment.getPolicy().getPolicyId())
                .policyNumber(payment.getPolicy().getPolicyNumber())
                .customerId(payment.getUser().getUserId())
                .customerName(payment.getUser().getFirstName() + " " + payment.getUser().getLastName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod().name())
                .transactionId(payment.getTransactionId())
                .paymentStatus(payment.getPaymentStatus().name())
                .message(message)
                .build();
    }
}

