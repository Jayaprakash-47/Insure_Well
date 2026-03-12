package com.healthshield.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthshield.dto.request.CustomerRegisterRequest;
import com.healthshield.dto.request.LoginRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.exception.BadRequestException;
import com.healthshield.exception.GlobalExceptionHandler;
import com.healthshield.exception.UnauthorizedException;
import com.healthshield.repository.UserRepository;
import com.healthshield.service.AuthService;
import com.healthshield.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    // ── helper to build a valid registration request ──
    private CustomerRegisterRequest validRegisterRequest() {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail("user@example.com");
        req.setPassword("Password123!");
        req.setPhone("9876543210");
        return req;
    }

    // ── 1. Register – success ──
    @Test
    void register_success_returnsCreatedAuthResponse() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("dummy-token")
                .role("CUSTOMER")
                .build();

        given(authService.register(any(CustomerRegisterRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("dummy-token")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    // ── 2. Register – validation failure (missing required fields) ──
    @Test
    void register_validationFailure_returns400WithFieldErrors() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest(); // Empty request

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message.email", notNullValue()));
    }


    // ── 3. Login – success ──
    @Test
    void login_success_returnsOkAuthResponse() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .token("login-token")
                .role("CUSTOMER")
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("login-token")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    // ── 4. Login – invalid credentials (UnauthorizedException → 401) ──
    @Test
    void login_unauthorized_returns401WithErrorBody() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new UnauthorizedException("Invalid credentials"));

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid credentials")));
    }
}

