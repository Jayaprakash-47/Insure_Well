//package com.healthshield.controller;
//
//import com.healthshield.dto.request.AgentSellPolicyRequest;
//import com.healthshield.dto.response.AgentDashboardResponse;
//import com.healthshield.dto.response.InsurancePlanResponse;
//import com.healthshield.dto.response.PolicyResponse;
//import com.healthshield.entity.User;
//import com.healthshield.service.AgentService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/agent")
//@PreAuthorize("hasRole('AGENT')")
//@RequiredArgsConstructor
//public class AgentController {
//
//    private final AgentService agentService;
//
//    @GetMapping("/dashboard")
//    public ResponseEntity<AgentDashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
//        return ResponseEntity.ok(agentService.getAgentDashboard(user.getUserId()));
//    }
//
//    @GetMapping("/profile")
//    public ResponseEntity<AgentDashboardResponse> getProfile(@AuthenticationPrincipal User user) {
//        return ResponseEntity.ok(agentService.getAgentProfile(user.getUserId()));
//    }
//
//    @GetMapping("/plans")
//    public ResponseEntity<List<InsurancePlanResponse>> getAvailablePlans() {
//        return ResponseEntity.ok(agentService.getAvailablePlans());
//    }
//
//    /** Get list of active customers — so agent can select who to sell to */
//    @GetMapping("/customers")
//    public ResponseEntity<List<Map<String, Object>>> getCustomersList() {
//        return ResponseEntity.ok(agentService.getCustomersList());
//    }
//
//    /** Sell a policy on behalf of a customer — core agent functionality */
//    @PostMapping("/sell-policy")
//    public ResponseEntity<PolicyResponse> sellPolicy(
//            @AuthenticationPrincipal User user,
//            @Valid @RequestBody AgentSellPolicyRequest request) {
//        return new ResponseEntity<>(agentService.sellPolicy(user.getUserId(), request), HttpStatus.CREATED);
//    }
//
//    /** Get policies sold by this agent */
//    @GetMapping("/my-policies")
//    public ResponseEntity<List<PolicyResponse>> getMyPolicies(@AuthenticationPrincipal User user) {
//        return ResponseEntity.ok(agentService.getMyPolicies(user.getUserId()));
//    }
//
//    @GetMapping("/policies")
//    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
//        return ResponseEntity.ok(agentService.getAllPolicies());
//    }
//
//    @GetMapping("/policies/status/{status}")
//    public ResponseEntity<List<PolicyResponse>> getPoliciesByStatus(@PathVariable String status) {
//        return ResponseEntity.ok(agentService.getPoliciesByStatus(status));
//    }
//}
