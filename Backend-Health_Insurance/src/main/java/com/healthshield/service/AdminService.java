package com.healthshield.service;

import com.healthshield.dto.request.AdminClaimDecisionRequest;
import com.healthshield.dto.request.CreateAgentRequest;
import com.healthshield.dto.request.CreateClaimsOfficerRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.dto.response.DashboardResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.PaymentStatus;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.repository.*;
import com.healthshield.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AgentRepository agentRepository;
    private final AdminRepository adminRepository;
    private final ClaimsOfficerRepository claimsOfficerRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final NetworkHospitalRepository networkHospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    // =================== USER MANAGEMENT ===================

    public AuthResponse createAgent(CreateAgentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        Agent agent = new Agent();
        agent.setFirstName(request.getFirstName());
        agent.setLastName(request.getLastName());
        agent.setEmail(request.getEmail());
        agent.setPassword(passwordEncoder.encode(request.getPassword()));
        agent.setPhone(request.getPhone());
        agent.setIsActive(true);
        agent.setLicenseNumber(request.getLicenseNumber());
        agent.setTerritory(request.getTerritory());
        agent.setCommissionPercentage(request.getCommissionPercentage());
        agent.setTotalPoliciesSold(0);

        Agent saved = agentRepository.save(agent);
        String token = jwtUtil.generateToken(saved);

        log.info("New agent created: {} ({})", saved.getEmail(), saved.getLicenseNumber());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(saved.getUserId())
                .firstName(saved.getFirstName())
                .email(saved.getEmail())
                .role("AGENT")
                .message("Agent created successfully")
                .build();
    }

    public AuthResponse createClaimsOfficer(CreateClaimsOfficerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }

        ClaimsOfficer officer = new ClaimsOfficer();
        officer.setFirstName(request.getFirstName());
        officer.setLastName(request.getLastName());
        officer.setEmail(request.getEmail());
        officer.setPassword(passwordEncoder.encode(request.getPassword()));
        officer.setPhone(request.getPhone());
        officer.setIsActive(true);
        officer.setEmployeeId(request.getEmployeeId());
        officer.setDepartment(request.getDepartment() != null ? request.getDepartment() : "Claims");
        officer.setSpecialization(request.getSpecialization() != null ? request.getSpecialization() : "General");
        officer.setTotalClaimsProcessed(0);
        officer.setTotalClaimsApproved(0);
        officer.setTotalClaimsRejected(0);
        officer.setApprovalLimit(request.getApprovalLimit() != null
                ? request.getApprovalLimit() : new BigDecimal("500000.00"));

        ClaimsOfficer saved = claimsOfficerRepository.save(officer);
        String token = jwtUtil.generateToken(saved);

        log.info("New Claims Officer created: {} ({})", saved.getEmail(), saved.getEmployeeId());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(saved.getUserId())
                .firstName(saved.getFirstName())
                .email(saved.getEmail())
                .role("CLAIMS_OFFICER")
                .message("Claims Officer created successfully")
                .build();
    }

    // =================== DASHBOARD ===================

    public DashboardResponse getDashboardStats() {
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalClaimsPaidOut = claimRepository.findAll().stream()
                .filter(c -> c.getClaimStatus() == ClaimStatus.SETTLED)
                .map(c -> c.getSettlementAmount() != null ? c.getSettlementAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalClaims = claimRepository.count();
        long approvedClaims = claimRepository.countByClaimStatus(ClaimStatus.APPROVED)
                + claimRepository.countByClaimStatus(ClaimStatus.PARTIALLY_APPROVED)
                + claimRepository.countByClaimStatus(ClaimStatus.SETTLED);
        BigDecimal settlementRatio = totalClaims > 0
                ? BigDecimal.valueOf(approvedClaims * 100.0 / totalClaims).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardResponse.builder()
                .totalCustomers(customerRepository.count())
                .totalAgents(agentRepository.count())
                .totalClaimsOfficers(claimsOfficerRepository.count())
                .totalAdmins(adminRepository.count())
                .totalPolicies(policyRepository.count())
                .totalActivePolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.ACTIVE).size())
                .totalPendingPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.PENDING).size())
                .totalExpiredPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.EXPIRED).size())
                .totalClaims(totalClaims)
                .totalPendingClaims(claimRepository.countByClaimStatus(ClaimStatus.SUBMITTED))
                .totalUnderReviewClaims(claimRepository.countByClaimStatus(ClaimStatus.UNDER_REVIEW))
                .totalApprovedClaims(claimRepository.countByClaimStatus(ClaimStatus.APPROVED)
                        + claimRepository.countByClaimStatus(ClaimStatus.PARTIALLY_APPROVED))
                .totalRejectedClaims(claimRepository.countByClaimStatus(ClaimStatus.REJECTED))
                .totalEscalatedClaims((long) claimRepository.findByIsEscalatedTrueAndEscalationResolvedByIsNull().size())
                .totalSettledClaims(claimRepository.countByClaimStatus(ClaimStatus.SETTLED))
                .totalPayments(paymentRepository.count())
                .totalRevenue(totalRevenue)
                .totalClaimsPaidOut(totalClaimsPaidOut)
                .claimSettlementRatio(settlementRatio)
                .totalActivePlans((long) insurancePlanRepository.findByIsActiveTrue().size())
                .totalNetworkHospitals(networkHospitalRepository.count())
                .build();
    }

    // =================== USER LISTING ===================

    public List<Map<String, Object>> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", c.getUserId());
                    map.put("firstName", c.getFirstName());
                    map.put("lastName", c.getLastName());
                    map.put("email", c.getEmail());
                    map.put("phone", c.getPhone());
                    map.put("isActive", c.getIsActive());
                    map.put("dateOfBirth", c.getDateOfBirth());
                    map.put("gender", c.getGender());
                    map.put("city", c.getCity());
                    map.put("state", c.getState());
                    map.put("createdAt", c.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", a.getUserId());
                    map.put("firstName", a.getFirstName());
                    map.put("lastName", a.getLastName());
                    map.put("email", a.getEmail());
                    map.put("phone", a.getPhone());
                    map.put("isActive", a.getIsActive());
                    map.put("licenseNumber", a.getLicenseNumber());
                    map.put("territory", a.getTerritory());
                    map.put("commissionPercentage", a.getCommissionPercentage());
                    map.put("totalPoliciesSold", a.getTotalPoliciesSold());
                    map.put("createdAt", a.getCreatedAt());

                    // Agent performance: total commission earned
                    BigDecimal totalCommission = policyRepository.findBySoldByAgentUserId(a.getUserId()).stream()
                            .map(p -> p.getCommissionAmount() != null ? p.getCommissionAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    map.put("totalCommissionEarned", totalCommission);

                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllClaimsOfficers() {
        return claimsOfficerRepository.findAll().stream()
                .map(co -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", co.getUserId());
                    map.put("firstName", co.getFirstName());
                    map.put("lastName", co.getLastName());
                    map.put("email", co.getEmail());
                    map.put("phone", co.getPhone());
                    map.put("isActive", co.getIsActive());
                    map.put("employeeId", co.getEmployeeId());
                    map.put("department", co.getDepartment());
                    map.put("specialization", co.getSpecialization());
                    map.put("totalClaimsProcessed", co.getTotalClaimsProcessed());
                    map.put("totalClaimsApproved", co.getTotalClaimsApproved());
                    map.put("totalClaimsRejected", co.getTotalClaimsRejected());
                    map.put("approvalLimit", co.getApprovalLimit());
                    map.put("createdAt", co.getCreatedAt());

                    int total = co.getTotalClaimsProcessed() != null ? co.getTotalClaimsProcessed() : 0;
                    int approved = co.getTotalClaimsApproved() != null ? co.getTotalClaimsApproved() : 0;
                    double rate = total > 0 ? (approved * 100.0 / total) : 0.0;
                    map.put("approvalRate", Math.round(rate * 100.0) / 100.0);

                    return map;
                })
                .collect(Collectors.toList());
    }

    // =================== USER ACTIVATION ===================

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {} ({})", user.getEmail(), userId);
    }

    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User activated: {} ({})", user.getEmail(), userId);
    }

    // =================== ESCALATED CLAIMS ===================

    /**
     * Get all escalated claims that need admin's attention.
     */
    public List<Map<String, Object>> getEscalatedClaims() {
        return claimRepository.findByIsEscalatedTrueAndEscalationResolvedByIsNull().stream()
                .map(claim -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("claimId", claim.getClaimId());
                    map.put("claimNumber", claim.getClaimNumber());
                    map.put("customerName", claim.getUser().getFirstName() + " " + claim.getUser().getLastName());
                    map.put("policyNumber", claim.getPolicy().getPolicyNumber());
                    map.put("claimAmount", claim.getClaimAmount());
                    map.put("diagnosis", claim.getDiagnosis());
                    map.put("hospitalName", claim.getHospitalName());
                    map.put("claimStatus", claim.getClaimStatus().name());
                    map.put("escalationReason", claim.getEscalationReason() != null ? claim.getEscalationReason().name() : null);
                    map.put("escalationNotes", claim.getEscalationNotes());
                    map.put("escalatedAt", claim.getEscalatedAt());
                    if (claim.getAssignedOfficer() != null) {
                        map.put("escalatedByOfficer", claim.getAssignedOfficer().getFirstName()
                                + " " + claim.getAssignedOfficer().getLastName());
                    }
                    map.put("reviewerRemarks", claim.getReviewerRemarks());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Admin makes the final decision on an escalated claim.
     */
    @Transactional
    public Map<String, Object> resolveEscalatedClaim(Long claimId, AdminClaimDecisionRequest request, User admin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

        if (!Boolean.TRUE.equals(claim.getIsEscalated())) {
            throw new BadRequestException("This claim was not escalated");
        }

        if (claim.getEscalationResolvedBy() != null) {
            throw new BadRequestException("This escalated claim has already been resolved");
        }

        String previousStatus = claim.getClaimStatus().name();
        ClaimStatus newStatus = ClaimStatus.valueOf(request.getDecision().toUpperCase());

        switch (newStatus) {
            case APPROVED:
                claim.setClaimStatus(ClaimStatus.APPROVED);
                claim.setApprovedAmount(request.getApprovedAmount() != null
                        ? request.getApprovedAmount() : claim.getClaimAmount());
                break;
            case PARTIALLY_APPROVED:
                if (request.getApprovedAmount() == null) {
                    throw new BadRequestException("Approved amount is required for partial approval");
                }
                claim.setClaimStatus(ClaimStatus.PARTIALLY_APPROVED);
                claim.setApprovedAmount(request.getApprovedAmount());
                break;
            case REJECTED:
                claim.setClaimStatus(ClaimStatus.REJECTED);
                claim.setRejectionReason(request.getRejectionReason());
                break;
            default:
                throw new BadRequestException("Invalid decision. Must be APPROVED, PARTIALLY_APPROVED, or REJECTED");
        }

        claim.setAdminRemarks(request.getAdminRemarks());
        claim.setEscalationResolvedBy(admin);
        claim.setEscalationResolvedAt(LocalDateTime.now());
        claim.setReviewedAt(LocalDateTime.now());

        // Update the officer's stats if the original officer is linked
        if (claim.getAssignedOfficer() != null) {
            ClaimsOfficer officer = claim.getAssignedOfficer();
            officer.setTotalClaimsProcessed(officer.getTotalClaimsProcessed() + 1);
            if (newStatus == ClaimStatus.APPROVED || newStatus == ClaimStatus.PARTIALLY_APPROVED) {
                officer.setTotalClaimsApproved(officer.getTotalClaimsApproved() + 1);
            } else {
                officer.setTotalClaimsRejected(officer.getTotalClaimsRejected() + 1);
            }
            claimsOfficerRepository.save(officer);
        }

        claimRepository.save(claim);

        auditService.logStatusChange("CLAIM", claimId, previousStatus, newStatus.name(),
                "Escalated claim resolved by Admin. Remarks: " + request.getAdminRemarks(), admin);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Escalated claim resolved successfully");
        response.put("claimId", claimId);
        response.put("claimNumber", claim.getClaimNumber());
        response.put("decision", newStatus.name());
        response.put("approvedAmount", claim.getApprovedAmount());
        response.put("adminRemarks", claim.getAdminRemarks());

        log.info("Escalated claim {} resolved by admin. Decision: {}", claim.getClaimNumber(), newStatus);

        return response;
    }

    // =================== AGENT PERFORMANCE ===================

    /**
     * Get agent performance leaderboard.
     */
    public List<Map<String, Object>> getAgentPerformance() {
        return agentRepository.findAll().stream()
                .map(agent -> {
                    Map<String, Object> perf = new HashMap<>();
                    perf.put("agentId", agent.getUserId());
                    perf.put("agentName", agent.getFirstName() + " " + agent.getLastName());
                    perf.put("territory", agent.getTerritory());

                    List<Policy> policies = policyRepository.findBySoldByAgentUserId(agent.getUserId());
                    perf.put("totalPoliciesSold", policies.size());

                    BigDecimal totalPremium = policies.stream()
                            .map(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    perf.put("totalPremiumGenerated", totalPremium);

                    BigDecimal totalCommission = policies.stream()
                            .map(p -> p.getCommissionAmount() != null ? p.getCommissionAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    perf.put("totalCommissionEarned", totalCommission);

                    long activePolicies = policies.stream()
                            .filter(p -> p.getPolicyStatus() == PolicyStatus.ACTIVE)
                            .count();
                    perf.put("activePolicies", activePolicies);

                    return perf;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("totalPremiumGenerated"))
                        .compareTo((BigDecimal) a.get("totalPremiumGenerated")))
                .collect(Collectors.toList());
    }
}
