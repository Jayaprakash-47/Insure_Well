//package com.healthshield.service;
//
//import com.healthshield.dto.request.AgentSellPolicyRequest;
//import com.healthshield.dto.request.PolicyMemberRequest;
//import com.healthshield.dto.response.AgentDashboardResponse;
//import com.healthshield.dto.response.InsurancePlanResponse;
//import com.healthshield.dto.response.PolicyResponse;
//import com.healthshield.dto.response.PolicyMemberResponse;
//import com.healthshield.entity.*;
//import com.healthshield.enums.Gender;
//import com.healthshield.enums.PolicyStatus;
//import com.healthshield.enums.Relationship;
//import com.healthshield.exception.BadRequestException;
//import com.healthshield.exception.ResourceNotFoundException;
//import com.healthshield.repository.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDate;
//import java.time.Period;
//import java.time.Year;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AgentService {
//
//    private final AgentRepository agentRepository;
//    private final PolicyRepository policyRepository;
//    private final PolicyMemberRepository policyMemberRepository;
//    private final InsurancePlanRepository insurancePlanRepository;
//    private final PremiumQuoteRepository premiumQuoteRepository;
//    private final UserRepository userRepository;
//    private final CustomerRepository customerRepository;
//    private final InsurancePlanService insurancePlanService;
//    private final AuditService auditService;
//
//    // =================== DASHBOARD ===================
//
//    public AgentDashboardResponse getAgentDashboard(Long agentId) {
//        Agent agent = agentRepository.findById(agentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
//
//        List<Policy> agentPolicies = policyRepository.findBySoldByAgentUserId(agentId);
//
//        BigDecimal totalCommission = agentPolicies.stream()
//                .map(p -> p.getCommissionAmount() != null ? p.getCommissionAmount() : BigDecimal.ZERO)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        long customersServed = agentPolicies.stream()
//                .map(p -> p.getUser().getUserId())
//                .distinct()
//                .count();
//
//        long activePolicies = agentPolicies.stream()
//                .filter(p -> p.getPolicyStatus() == PolicyStatus.ACTIVE)
//                .count();
//
//        BigDecimal totalPremiumGenerated = agentPolicies.stream()
//                .map(p -> p.getPremiumAmount() != null ? p.getPremiumAmount() : BigDecimal.ZERO)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return AgentDashboardResponse.builder()
//                .agentId(agent.getUserId())
//                .agentName(agent.getFirstName() + " " + agent.getLastName())
//                .email(agent.getEmail())
//                .licenseNumber(agent.getLicenseNumber())
//                .territory(agent.getTerritory())
//                .commissionPercentage(agent.getCommissionPercentage())
//                .totalPoliciesSold(agentPolicies.size())
//                .totalCommissionEarned(totalCommission)
//                .totalCustomersServed(customersServed)
//                .activePolicies(activePolicies)
//                .totalPremiumGenerated(totalPremiumGenerated)
//                .build();
//    }
//
//    public AgentDashboardResponse getAgentProfile(Long agentId) {
//        return getAgentDashboard(agentId);
//    }
//
//    // =================== SELL POLICY ===================
//
//    /**
//     * Agent sells a policy on behalf of a customer.
//     * This is the core agent functionality — they recommend a plan and create the policy.
//     */
//    @Transactional
//    public PolicyResponse sellPolicy(Long agentId, AgentSellPolicyRequest request) {
//        Agent agent = agentRepository.findById(agentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + agentId));
//
//        User customer = userRepository.findById(request.getCustomerId())
//                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
//
//        // Verify the target user is actually a customer
//        if (!(customer instanceof Customer)) {
//            throw new BadRequestException("User with id " + request.getCustomerId() + " is not a customer");
//        }
//
//        InsurancePlan plan = insurancePlanRepository.findById(request.getPlanId())
//                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + request.getPlanId()));
//
//        if (!plan.getIsActive()) {
//            throw new BadRequestException("Insurance plan '" + plan.getPlanName() + "' is not active");
//        }
//
//        // Calculate premium
//        BigDecimal calculatedPremium = calculatePremiumForSale(customer, plan, request);
//
//        // Calculate agent commission
//        BigDecimal commissionAmount = calculatedPremium
//                .multiply(agent.getCommissionPercentage())
//                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
//
//        String policyNumber = generatePolicyNumber();
//
//        Policy policy = Policy.builder()
//                .policyNumber(policyNumber)
//                .user(customer)
//                .plan(plan)
//                .soldByAgent(agent)
//                .premiumAmount(calculatedPremium)
//                .coverageAmount(plan.getCoverageAmount())
//                .remainingCoverage(plan.getCoverageAmount())
//                .totalClaimedAmount(BigDecimal.ZERO)
//                .policyStatus(PolicyStatus.PENDING)
//                .nomineeName(request.getNomineeName())
//                .nomineeRelationship(request.getNomineeRelationship())
//                .commissionAmount(commissionAmount)
//                .renewalCount(0)
//                .noClaimBonus(BigDecimal.ZERO)
//                .members(new ArrayList<>())
//                .claims(new ArrayList<>())
//                .payments(new ArrayList<>())
//                .build();
//
//        Policy savedPolicy = policyRepository.save(policy);
//
//        // Add members if provided
//        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
//            for (PolicyMemberRequest memberReq : request.getMembers()) {
//                PolicyMember member = PolicyMember.builder()
//                        .policy(savedPolicy)
//                        .memberName(memberReq.getMemberName())
//                        .relationship(Relationship.valueOf(memberReq.getRelationship().toUpperCase()))
//                        .dateOfBirth(memberReq.getDateOfBirth())
//                        .gender(memberReq.getGender() != null
//                                ? Gender.valueOf(memberReq.getGender().toUpperCase()) : null)
//                        .preExistingDiseases(memberReq.getPreExistingDiseases())
//                        .build();
//                policyMemberRepository.save(member);
//            }
//        }
//
//        // Update agent's sold count
//        agent.setTotalPoliciesSold(agent.getTotalPoliciesSold() + 1);
//        agentRepository.save(agent);
//
//        // Reload to include members
//        savedPolicy = policyRepository.findById(savedPolicy.getPolicyId()).orElse(savedPolicy);
//
//        auditService.logCreation("POLICY", savedPolicy.getPolicyId(),
//                "Policy sold by agent " + agent.getFirstName() + " " + agent.getLastName()
//                        + " | Customer: " + customer.getFirstName() + " " + customer.getLastName()
//                        + " | Plan: " + plan.getPlanName()
//                        + " | Premium: ₹" + calculatedPremium
//                        + " | Commission: ₹" + commissionAmount,
//                agent);
//
//        log.info("Agent {} sold policy {} to customer {} | Premium: ₹{} | Commission: ₹{}",
//                agent.getLicenseNumber(), policyNumber, customer.getEmail(),
//                calculatedPremium, commissionAmount);
//
//        return mapToResponse(savedPolicy);
//    }
//
//    // =================== BROWSE ===================
//
//    public List<InsurancePlanResponse> getAvailablePlans() {
//        return insurancePlanService.getAllActivePlans();
//    }
//
//    public List<PolicyResponse> getMyPolicies(Long agentId) {
//        return policyRepository.findBySoldByAgentUserId(agentId).stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    public List<PolicyResponse> getAllPolicies() {
//        return policyRepository.findAll().stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    public List<PolicyResponse> getPoliciesByStatus(String status) {
//        PolicyStatus policyStatus = PolicyStatus.valueOf(status.toUpperCase());
//        return policyRepository.findByPolicyStatus(policyStatus).stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get customers list so agent can select a customer to sell to.
//     */
//    public List<java.util.Map<String, Object>> getCustomersList() {
//        return customerRepository.findAll().stream()
//                .filter(c -> c.getIsActive() != null && c.getIsActive())
//                .map(c -> {
//                    java.util.Map<String, Object> map = new java.util.HashMap<>();
//                    map.put("userId", c.getUserId());
//                    map.put("firstName", c.getFirstName());
//                    map.put("lastName", c.getLastName());
//                    map.put("email", c.getEmail());
//                    map.put("phone", c.getPhone());
//                    map.put("dateOfBirth", c.getDateOfBirth());
//                    map.put("city", c.getCity());
//                    map.put("state", c.getState());
//                    return map;
//                })
//                .collect(Collectors.toList());
//    }
//
//    // =================== PREMIUM CALCULATION ===================
//
//    private BigDecimal calculatePremiumForSale(User user, InsurancePlan plan, AgentSellPolicyRequest request) {
//        // If quote ID is provided, use pre-calculated premium
//        if (request.getQuoteId() != null) {
//            PremiumQuote quote = premiumQuoteRepository.findById(request.getQuoteId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Quote not found"));
//            if (!quote.getPlan().getPlanId().equals(plan.getPlanId())) {
//                throw new BadRequestException("Quote is for a different plan");
//            }
//            return quote.getCalculatedPremium();
//        }
//
//        // Fallback: age-based calculation
//        int age = 30;
//        if (user instanceof Customer customer) {
//            if (customer.getDateOfBirth() != null) {
//                age = Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears();
//            }
//        }
//
//        double ageFactor;
//        if (age <= 30) ageFactor = 1.0;
//        else if (age <= 40) ageFactor = 1.2;
//        else if (age <= 50) ageFactor = 1.5;
//        else if (age <= 60) ageFactor = 1.8;
//        else ageFactor = 2.2;
//
//        int members = (request.getMembers() != null) ? request.getMembers().size() + 1 : 1;
//        double memberFactor = 1.0 + (members - 1) * 0.7;
//
//        return plan.getBasePremiumAmount()
//                .multiply(BigDecimal.valueOf(ageFactor))
//                .multiply(BigDecimal.valueOf(memberFactor))
//                .setScale(2, RoundingMode.HALF_UP);
//    }
//
//    private String generatePolicyNumber() {
//        String number;
//        Random random = new Random();
//        do {
//            number = "HHS-" + Year.now().getValue() + "-" + String.format("%06d", random.nextInt(999999));
//        } while (policyRepository.existsByPolicyNumber(number));
//        return number;
//    }
//
//    // =================== MAPPER ===================
//
//    private PolicyResponse mapToResponse(Policy policy) {
//        List<PolicyMemberResponse> memberResponses = new ArrayList<>();
//        if (policy.getMembers() != null) {
//            memberResponses = policy.getMembers().stream()
//                    .map(member -> PolicyMemberResponse.builder()
//                            .memberId(member.getMemberId())
//                            .memberName(member.getMemberName())
//                            .relationship(member.getRelationship() != null ? member.getRelationship().name() : null)
//                            .dateOfBirth(member.getDateOfBirth())
//                            .gender(member.getGender() != null ? member.getGender().name() : null)
//                            .preExistingDiseases(member.getPreExistingDiseases())
//                            .build())
//                    .collect(Collectors.toList());
//        }
//
//        PolicyResponse.PolicyResponseBuilder builder = PolicyResponse.builder()
//                .policyId(policy.getPolicyId())
//                .policyNumber(policy.getPolicyNumber())
//                .customerId(policy.getUser().getUserId())
//                .customerName(policy.getUser().getFirstName() + " " + policy.getUser().getLastName())
//                .planId(policy.getPlan().getPlanId())
//                .planName(policy.getPlan().getPlanName())
//                .premiumAmount(policy.getPremiumAmount())
//                .coverageAmount(policy.getCoverageAmount())
//                .remainingCoverage(policy.getRemainingCoverage())
//                .totalClaimedAmount(policy.getTotalClaimedAmount())
//                .startDate(policy.getStartDate())
//                .endDate(policy.getEndDate())
//                .policyStatus(policy.getPolicyStatus().name())
//                .nomineeName(policy.getNomineeName())
//                .nomineeRelationship(policy.getNomineeRelationship())
//                .createdAt(policy.getCreatedAt())
//                .members(memberResponses)
//                .commissionAmount(policy.getCommissionAmount())
//                .renewalCount(policy.getRenewalCount())
//                .noClaimBonus(policy.getNoClaimBonus());
//
//        if (policy.getSoldByAgent() != null) {
//            builder.agentId(policy.getSoldByAgent().getUserId())
//                    .agentName(policy.getSoldByAgent().getFirstName() + " " + policy.getSoldByAgent().getLastName());
//        }
//
//        if (policy.getOriginalPolicy() != null) {
//            builder.originalPolicyId(policy.getOriginalPolicy().getPolicyId());
//        }
//
//        return builder.build();
//    }
//}
