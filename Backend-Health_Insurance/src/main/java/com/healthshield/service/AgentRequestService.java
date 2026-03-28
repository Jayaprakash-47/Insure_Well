package com.healthshield.service;

import com.healthshield.dto.request.AgentRequestCreateDTO;
import com.healthshield.dto.request.PolicyPurchaseRequest;
import com.healthshield.dto.response.AgentRequestResponse;
import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.AgentRequestStatus;
import com.healthshield.enums.NotificationType;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRequestService {

    private final AgentRequestRepository agentRequestRepository;
    private final UserRepository userRepository;
    private final UnderwriterRepository underwriterRepository;
    private final PolicyService policyService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // ── Customer creates a request ────────────────────────────────────────

    @Transactional
    public AgentRequestResponse createRequest(Long customerId,
                                              AgentRequestCreateDTO dto) {
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found: " + customerId));

        if (!(user instanceof Customer customer)) {
            throw new BadRequestException("Only customers can create agent requests");
        }

        AgentRequest request = AgentRequest.builder()
                .customer(customer)
                .planTypeInterest(dto.getPlanTypeInterest())
                .preferredTime(dto.getPreferredTime())
                .message(dto.getMessage())
                .status(AgentRequestStatus.PENDING)
                .build();

        AgentRequest saved = agentRequestRepository.save(request);

        // Notify all underwriters about the new request
        underwriterRepository.findAll().forEach(uw ->
                notificationService.sendNotification(
                        uw.getEmail(),
                        "📋 New agent request from " + customer.getFirstName()
                                + " " + customer.getLastName()
                                + " — interested in " + dto.getPlanTypeInterest()
                                + " plan.",
                        NotificationType.GENERAL
                )
        );

        auditLogService.log(
                "AGENT_REQUEST_CREATED", "CUSTOMER", customer.getEmail(),
                "Agent request created for " + dto.getPlanTypeInterest() + " plan"
        );

        log.info("Agent request #{} created by customer {}",
                saved.getId(), customerId);
        return mapToResponse(saved);
    }

    // ── Customer gets their own requests ─────────────────────────────────

    public List<AgentRequestResponse> getMyRequests(Long customerId) {
        return agentRequestRepository
                .findByCustomerUserIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Customer cancels a request ────────────────────────────────────────

    @Transactional
    public AgentRequestResponse cancelRequest(Long customerId, Long requestId) {
        AgentRequest request = agentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found: " + requestId));

        if (!request.getCustomer().getUserId().equals(customerId)) {
            throw new BadRequestException("You can only cancel your own requests");
        }
        if (request.getStatus() == AgentRequestStatus.COMPLETED) {
            throw new BadRequestException("Completed requests cannot be cancelled");
        }

        request.setStatus(AgentRequestStatus.CANCELLED);
        return mapToResponse(agentRequestRepository.save(request));
    }

    // ── Underwriter gets all PENDING requests ─────────────────────────────

    public List<AgentRequestResponse> getPendingRequests() {
        return agentRequestRepository
                .findByStatusOrderByCreatedAtAsc(AgentRequestStatus.PENDING)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Underwriter gets their accepted requests ──────────────────────────

    public List<AgentRequestResponse> getMyAcceptedRequests(Long underwriterId) {
        return agentRequestRepository
                .findByAssignedUnderwriterUserIdOrderByCreatedAtDesc(underwriterId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Underwriter accepts a request ─────────────────────────────────────

    @Transactional
    public AgentRequestResponse acceptRequest(Long underwriterId, Long requestId) {
        AgentRequest request = agentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found: " + requestId));

        if (request.getStatus() != AgentRequestStatus.PENDING) {
            throw new BadRequestException(
                    "Only PENDING requests can be accepted");
        }

        Underwriter underwriter = underwriterRepository.findById(underwriterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Underwriter not found: " + underwriterId));

        request.setAssignedUnderwriter(underwriter);
        request.setStatus(AgentRequestStatus.ACCEPTED);
        request.setAcceptedAt(LocalDateTime.now());

        AgentRequest saved = agentRequestRepository.save(request);

        // Notify customer
        notificationService.sendNotification(
                request.getCustomer().getEmail(),
                "✅ Your agent request has been accepted by "
                        + underwriter.getFirstName() + " " + underwriter.getLastName()
                        + ". They will contact you at your preferred time.",
                NotificationType.GENERAL
        );

        auditLogService.log(
                "AGENT_REQUEST_ACCEPTED", "UNDERWRITER", underwriter.getEmail(),
                "Accepted agent request #" + requestId + " from "
                        + request.getCustomer().getFirstName()
        );

        return mapToResponse(saved);
    }

    // ── Underwriter applies for customer ──────────────────────────────────

    @Transactional
    public PolicyResponse applyForCustomer(Long underwriterId,
                                           Long requestId,
                                           PolicyPurchaseRequest policyRequest) {
        AgentRequest agentRequest = agentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Agent request not found: " + requestId));

        if (agentRequest.getStatus() != AgentRequestStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Request must be ACCEPTED before applying");
        }

        if (agentRequest.getAssignedUnderwriter() == null
                || !agentRequest.getAssignedUnderwriter()
                .getUserId().equals(underwriterId)) {
            throw new BadRequestException(
                    "This request is not assigned to you");
        }

        Long customerId = agentRequest.getCustomer().getUserId();

        // Apply policy on behalf of the customer
        PolicyResponse policy = policyService.purchasePolicy(
                customerId, policyRequest);

        // Mark request as completed
        agentRequest.setStatus(AgentRequestStatus.COMPLETED);
        agentRequest.setCompletedAt(LocalDateTime.now());
        agentRequestRepository.save(agentRequest);

        Underwriter underwriter = agentRequest.getAssignedUnderwriter();

        // Notify customer
        notificationService.sendNotification(
                agentRequest.getCustomer().getEmail(),
                "📋 Policy " + policy.getPolicyNumber()
                        + " has been submitted on your behalf by agent "
                        + underwriter.getFirstName() + " "
                        + underwriter.getLastName()
                        + ". Please log in to review.",
                NotificationType.GENERAL
        );

        auditLogService.log(
                "POLICY_APPLIED_BY_AGENT", "UNDERWRITER", underwriter.getEmail(),
                "Policy " + policy.getPolicyNumber()
                        + " applied for customer "
                        + agentRequest.getCustomer().getEmail()
        );

        log.info("Underwriter {} applied policy {} for customer {}",
                underwriterId, policy.getPolicyNumber(), customerId);

        return policy;
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    private AgentRequestResponse mapToResponse(AgentRequest r) {
        AgentRequestResponse.AgentRequestResponseBuilder b =
                AgentRequestResponse.builder()
                        .id(r.getId())
                        .customerId(r.getCustomer().getUserId())
                        .customerName(r.getCustomer().getFirstName()
                                + " " + r.getCustomer().getLastName())
                        .customerEmail(r.getCustomer().getEmail())
                        .customerPhone(r.getCustomer().getPhone())
                        .planTypeInterest(r.getPlanTypeInterest())
                        .preferredTime(r.getPreferredTime())
                        .message(r.getMessage())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt())
                        .acceptedAt(r.getAcceptedAt())
                        .completedAt(r.getCompletedAt());

        if (r.getAssignedUnderwriter() != null) {
            b.underwriterId(r.getAssignedUnderwriter().getUserId())
                    .underwriterName(r.getAssignedUnderwriter().getFirstName()
                            + " " + r.getAssignedUnderwriter().getLastName());
        }

        if (r.getResultingPolicy() != null) {
            b.resultingPolicyId(r.getResultingPolicy().getPolicyId())
                    .resultingPolicyNumber(r.getResultingPolicy().getPolicyNumber());
        }

        return b.build();
    }
}