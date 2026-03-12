package com.healthshield.controller;

import com.healthshield.dto.response.PolicyResponse;
import com.healthshield.entity.User;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.GlobalExceptionHandler;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.repository.UserRepository;
import com.healthshield.service.PolicyService;
import com.healthshield.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PolicyController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyService policyService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("customer@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Place the User into the SecurityContext so @AuthenticationPrincipal resolves it
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ── helper ──
    private PolicyResponse samplePolicyResponse(Long id, String policyNumber) {
        return PolicyResponse.builder()
                .policyId(id)
                .policyNumber(policyNumber)
                .premiumAmount(BigDecimal.valueOf(5000))
                .coverageAmount(BigDecimal.valueOf(500000))
                .policyStatus("ACTIVE")
                .build();
    }

    // ── 1. GET /api/policies/my-policies – success ──
    @Test
    void getMyPolicies_success_returnsList() throws Exception {
        PolicyResponse p1 = samplePolicyResponse(1L, "POL-001");
        PolicyResponse p2 = samplePolicyResponse(2L, "POL-002");

        given(policyService.getPoliciesByUser(1L)).willReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/policies/my-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].policyId", is(1)))
                .andExpect(jsonPath("$[0].policyNumber", is("POL-001")));
    }

    // ── 2. GET /api/policies/{id} – success ──
    @Test
    void getPolicyById_success_returnsPolicy() throws Exception {
        PolicyResponse resp = samplePolicyResponse(10L, "POL-010");
        given(policyService.getPolicyById(any(User.class), eq(10L))).willReturn(resp);

        mockMvc.perform(get("/api/policies/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId", is(10)))
                .andExpect(jsonPath("$.policyNumber", is("POL-010")))
                .andExpect(jsonPath("$.policyStatus", is("ACTIVE")));
    }

    // ── 3. GET /api/policies/{id} – not found (ResourceNotFoundException → 404) ──
    @Test
    void getPolicyById_notFound_returns404() throws Exception {
        given(policyService.getPolicyById(any(User.class), eq(99L)))
                .willThrow(new ResourceNotFoundException("Policy not found"));

        mockMvc.perform(get("/api/policies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Policy not found")));
    }

    // ── 4. GET /api/policies – get all (admin/underwriter) ──
    @Test
    void getAllPolicies_success_returnsList() throws Exception {
        PolicyResponse p = samplePolicyResponse(5L, "POL-005");
        given(policyService.getAllPolicies()).willReturn(List.of(p));

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].policyId", is(5)));
    }
}

