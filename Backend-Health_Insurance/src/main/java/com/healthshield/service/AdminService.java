package com.healthshield.service;

import com.healthshield.dto.request.AdminAssignUnderwriterRequest;
import com.healthshield.dto.request.AdminAssignClaimsOfficerRequest;
import com.healthshield.dto.request.CreateUnderwriterRequest;
import com.healthshield.dto.request.CreateClaimsOfficerRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.dto.response.DashboardResponse;
import com.healthshield.entity.*;
import com.healthshield.enums.ClaimStatus;
import com.healthshield.enums.NotificationType;
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
    private final UnderwriterRepository underwriterRepository;
    private final AdminRepository adminRepository;
    private final ClaimsOfficerRepository claimsOfficerRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    // =================== USER MANAGEMENT ===================

    public AuthResponse createUnderwriter(CreateUnderwriterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }
        Underwriter underwriter = new Underwriter();
        underwriter.setFirstName(request.getFirstName());
        underwriter.setLastName(request.getLastName());
        underwriter.setEmail(request.getEmail());
        underwriter.setPassword(passwordEncoder.encode(request.getPassword()));
        underwriter.setPhone(request.getPhone());
        underwriter.setIsActive(true);
        underwriter.setLicenseNumber(request.getLicenseNumber());
        underwriter.setSpecialization(request.getSpecialization());
        underwriter.setCommissionPercentage(request.getCommissionPercentage());
        underwriter.setTotalQuotesSent(0);
        Underwriter saved = underwriterRepository.save(underwriter);
        String token = jwtUtil.generateToken(saved);
        log.info("New Underwriter created: {} ({})", saved.getEmail(), saved.getLicenseNumber());
        return AuthResponse.builder().token(token).type("Bearer").userId(saved.getUserId())
                .firstName(saved.getFirstName()).email(saved.getEmail()).role("UNDERWRITER")
                .message("Underwriter created successfully").build();
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
        officer.setApprovalLimit(request.getApprovalLimit() != null ? request.getApprovalLimit() : new BigDecimal("500000.00"));
        ClaimsOfficer savedOfficer = claimsOfficerRepository.save(officer);
        String token = jwtUtil.generateToken(savedOfficer);
        log.info("New Claims Officer created: {} ({})", savedOfficer.getEmail(), savedOfficer.getEmployeeId());
        return AuthResponse.builder().token(token).type("Bearer").userId(savedOfficer.getUserId())
                .firstName(savedOfficer.getFirstName()).email(savedOfficer.getEmail()).role("CLAIMS_OFFICER")
                .message("Claims Officer created successfully").build();
    }

    // =================== DASHBOARD ===================

    public DashboardResponse getDashboardStats() {
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .totalUnderwriters(underwriterRepository.count())
                .totalClaimsOfficers(claimsOfficerRepository.count())
                .totalAdmins(adminRepository.count())
                .totalPolicies(policyRepository.count())
                .totalActivePolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.ACTIVE).size())
                .totalPendingPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.PENDING).size())
                .totalAssignedPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.ASSIGNED).size())
                .totalQuoteSentPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.QUOTE_SENT).size())
                .totalExpiredPolicies((long) policyRepository.findByPolicyStatus(PolicyStatus.EXPIRED).size())
                .totalClaims(totalClaims)
                .totalPendingClaims(claimRepository.countByClaimStatus(ClaimStatus.SUBMITTED))
                .totalUnderReviewClaims(claimRepository.countByClaimStatus(ClaimStatus.UNDER_REVIEW))
                .totalApprovedClaims(claimRepository.countByClaimStatus(ClaimStatus.APPROVED)
                        + claimRepository.countByClaimStatus(ClaimStatus.PARTIALLY_APPROVED))
                .totalRejectedClaims(claimRepository.countByClaimStatus(ClaimStatus.REJECTED))
                .totalSettledClaims(claimRepository.countByClaimStatus(ClaimStatus.SETTLED))
                .totalPayments(paymentRepository.count())
                .totalRevenue(totalRevenue)
                .totalClaimsPaidOut(totalClaimsPaidOut)
                .claimSettlementRatio(settlementRatio)
                .totalActivePlans((long) insurancePlanRepository.findByIsActiveTrue().size())
                .build();
    }

    // =================== USER LISTING ===================

    public List<Map<String, Object>> getAllCustomers() {
        return customerRepository.findAll().stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", c.getUserId()); map.put("firstName", c.getFirstName());
            map.put("lastName", c.getLastName()); map.put("email", c.getEmail());
            map.put("phone", c.getPhone()); map.put("isActive", c.getIsActive());
            map.put("dateOfBirth", c.getDateOfBirth()); map.put("gender", c.getGender());
            map.put("city", c.getCity()); map.put("state", c.getState());
            map.put("createdAt", c.getCreatedAt()); return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllUnderwriters() {
        return underwriterRepository.findAll().stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getUserId()); map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName()); map.put("email", u.getEmail());
            map.put("phone", u.getPhone()); map.put("isActive", u.getIsActive());
            map.put("licenseNumber", u.getLicenseNumber()); map.put("specialization", u.getSpecialization());
            map.put("commissionPercentage", u.getCommissionPercentage());
            map.put("totalQuotesSent", u.getTotalQuotesSent()); map.put("createdAt", u.getCreatedAt());
            BigDecimal totalCommission = policyRepository.findByAssignedUnderwriterUserId(u.getUserId()).stream()
                    .map(p -> p.getCommissionAmount() != null ? p.getCommissionAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            map.put("totalCommissionEarned", totalCommission); return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllClaimsOfficers() {
        return claimsOfficerRepository.findAll().stream().map(co -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", co.getUserId()); map.put("firstName", co.getFirstName());
            map.put("lastName", co.getLastName()); map.put("email", co.getEmail());
            map.put("phone", co.getPhone()); map.put("isActive", co.getIsActive());
            map.put("employeeId", co.getEmployeeId()); map.put("department", co.getDepartment());
            map.put("specialization", co.getSpecialization());
            map.put("totalClaimsProcessed", co.getTotalClaimsProcessed());
            map.put("totalClaimsApproved", co.getTotalClaimsApproved());
            map.put("totalClaimsRejected", co.getTotalClaimsRejected());
            map.put("approvalLimit", co.getApprovalLimit()); map.put("createdAt", co.getCreatedAt());
            int total = co.getTotalClaimsProcessed() != null ? co.getTotalClaimsProcessed() : 0;
            int approved = co.getTotalClaimsApproved() != null ? co.getTotalClaimsApproved() : 0;
            map.put("approvalRate", Math.round((total > 0 ? (approved * 100.0 / total) : 0.0) * 100.0) / 100.0);
            return map;
        }).collect(Collectors.toList());
    }

    // =================== USER ACTIVATION ===================

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(false); userRepository.save(user);
        log.info("User deactivated: {} ({})", user.getEmail(), userId);
    }

    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(true); userRepository.save(user);
        log.info("User activated: {} ({})", user.getEmail(), userId);
    }

    // =================== ASSIGN UNDERWRITER TO POLICY ===================

    @Transactional
    public Map<String, Object> assignUnderwriter(Long policyId, AdminAssignUnderwriterRequest request, User admin) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));
        if (policy.getPolicyStatus() != PolicyStatus.PENDING && policy.getPolicyStatus() != PolicyStatus.ASSIGNED) {
            throw new BadRequestException("Can only assign underwriter to PENDING or ASSIGNED policies. Current: " + policy.getPolicyStatus());
        }
        Underwriter underwriter = underwriterRepository.findById(request.getUnderwriterId())
                .orElseThrow(() -> new ResourceNotFoundException("Underwriter not found with id: " + request.getUnderwriterId()));
        if (!Boolean.TRUE.equals(underwriter.getIsActive())) {
            throw new BadRequestException("Cannot assign to an inactive underwriter");
        }
        
        boolean isReassignment = policy.getAssignedUnderwriter() != null;
        
        policy.setAssignedUnderwriter(underwriter);
        policy.setAssignedAt(LocalDateTime.now());
        
        if (!isReassignment) {
            policy.setPolicyStatus(PolicyStatus.ASSIGNED);
        }
        notificationService.sendNotification(
                underwriter.getEmail(),
                "New policy " + policy.getPolicyNumber()
                        + " assigned to you for review.",
                NotificationType.GENERAL
        );

        policyRepository.save(policy);
        log.info("Policy {} assigned to underwriter {}", policy.getPolicyNumber(), underwriter.getUserId());
        return Map.of("message", "Underwriter assigned successfully", "policyId", policyId,
                "policyNumber", policy.getPolicyNumber(), "underwriterId", underwriter.getUserId(),
                "underwriterName", underwriter.getFirstName() + " " + underwriter.getLastName(),
                "policyStatus", PolicyStatus.ASSIGNED.name());

    }

    // =================== ASSIGN CLAIMS OFFICER TO CLAIM ===================

    @Transactional
    public Map<String, Object> assignClaimsOfficer(Long claimId, AdminAssignClaimsOfficerRequest request, User admin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));
        if (claim.getClaimStatus() != ClaimStatus.SUBMITTED && claim.getClaimStatus() != ClaimStatus.UNDER_REVIEW) {
            throw new BadRequestException("Can only assign officer to SUBMITTED or UNDER_REVIEW claims. Current: " + claim.getClaimStatus());
        }
        ClaimsOfficer officer = claimsOfficerRepository.findById(request.getClaimsOfficerId())
                .orElseThrow(() -> new ResourceNotFoundException("Claims Officer not found with id: " + request.getClaimsOfficerId()));
        if (!Boolean.TRUE.equals(officer.getIsActive())) {
            throw new BadRequestException("Cannot assign to an inactive Claims Officer");
        }
        
        boolean isReassignment = claim.getAssignedOfficer() != null;
        
        claim.setAssignedOfficer(officer);
        if (!isReassignment) {
            claim.setClaimStatus(ClaimStatus.UNDER_REVIEW);
            claim.setReviewStartedAt(LocalDateTime.now());
        }
        
        claimRepository.save(claim);
        log.info("Claim {} assigned to officer {}", claim.getClaimNumber(), officer.getEmployeeId());
        return Map.of("message", "Claims Officer assigned successfully", "claimId", claimId,
                "claimNumber", claim.getClaimNumber(), "officerId", officer.getUserId(),
                "officerName", officer.getFirstName() + " " + officer.getLastName(),
                "claimStatus", ClaimStatus.UNDER_REVIEW.name());
    }

    // =================== PENDING POLICY APPLICATIONS ===================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingPolicyApplications() {
        return policyRepository.findAll().stream()
                .filter(p -> p.getPolicyStatus() == PolicyStatus.PENDING || p.getPolicyStatus() == PolicyStatus.ASSIGNED || p.getPolicyStatus() == PolicyStatus.CONCERN_RAISED)
                .map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("policyId", p.getPolicyId()); map.put("policyNumber", p.getPolicyNumber());
            map.put("customerId", p.getUser().getUserId());
            map.put("customerName", p.getUser().getFirstName() + " " + p.getUser().getLastName());
            map.put("customerEmail", p.getUser().getEmail()); map.put("planName", p.getPlan().getPlanName());
            map.put("planType", p.getPlan().getPlanType().name()); map.put("coverageAmount", p.getCoverageAmount());
            map.put("policyStatus", p.getPolicyStatus().name()); map.put("createdAt", p.getCreatedAt());
            map.put("memberCount", p.getMembers() != null ? p.getMembers().size() : 0);
            
            if (p.getAssignedUnderwriter() != null) {
                map.put("assignedUnderwriterId", p.getAssignedUnderwriter().getUserId());
                map.put("assignedUnderwriterName", p.getAssignedUnderwriter().getFirstName() + " " + p.getAssignedUnderwriter().getLastName());
            }
            
            return map;
        }).collect(Collectors.toList());
    }

    // =================== SUBMITTED CLAIMS (UNASSIGNED) ===================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSubmittedClaims() {
        return claimRepository.findAll().stream()
                .filter(c -> c.getClaimStatus() == ClaimStatus.SUBMITTED || c.getClaimStatus() == ClaimStatus.UNDER_REVIEW)
                .map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("claimId", c.getClaimId()); map.put("claimNumber", c.getClaimNumber());
            map.put("customerName", c.getUser().getFirstName() + " " + c.getUser().getLastName());
            map.put("customerEmail", c.getUser().getEmail());
            map.put("policyNumber", c.getPolicy().getPolicyNumber()); map.put("claimAmount", c.getClaimAmount());
            map.put("claimType", c.getClaimType().name()); map.put("hospitalName", c.getHospitalName());
            map.put("diagnosis", c.getDiagnosis()); map.put("claimStatus", c.getClaimStatus().name());
            map.put("createdAt", c.getCreatedAt());
            
            if (c.getAssignedOfficer() != null) {
                map.put("assignedOfficerId", c.getAssignedOfficer().getUserId());
                map.put("assignedOfficerName", c.getAssignedOfficer().getFirstName() + " " + c.getAssignedOfficer().getLastName());
            }
            
            return map;
        }).collect(Collectors.toList());
    }


    // =================== UNDERWRITER PERFORMANCE ===================

    public List<Map<String, Object>> getUnderwriterPerformance() {
        return underwriterRepository.findAll().stream().map(u -> {
            Map<String, Object> perf = new HashMap<>();
            perf.put("underwriterId", u.getUserId());
            perf.put("underwriterName", u.getFirstName() + " " + u.getLastName());
            perf.put("specialization", u.getSpecialization());
            List<Policy> policies = policyRepository.findByAssignedUnderwriterUserId(u.getUserId());
            perf.put("totalQuotesSent", u.getTotalQuotesSent()); perf.put("totalPoliciesHandled", policies.size());
            BigDecimal totalPremium = policies.stream()
                    .map(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            perf.put("totalPremiumGenerated", totalPremium);
            BigDecimal totalCommission = policies.stream()
                    .map(p -> p.getCommissionAmount() != null ? p.getCommissionAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            perf.put("totalCommissionEarned", totalCommission);
            perf.put("activePolicies", policies.stream().filter(p -> p.getPolicyStatus() == PolicyStatus.ACTIVE).count());
            return perf;
        }).sorted((a, b) -> ((BigDecimal) b.get("totalPremiumGenerated")).compareTo((BigDecimal) a.get("totalPremiumGenerated")))
                .collect(Collectors.toList());
    }
}
