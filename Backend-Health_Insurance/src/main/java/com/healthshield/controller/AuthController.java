package com.healthshield.controller;

import com.healthshield.dto.request.CustomerRegisterRequest;
import com.healthshield.dto.request.LoginRequest;
import com.healthshield.dto.response.AuthResponse;
import com.healthshield.service.AuthService;
import com.healthshield.service.ForgotPasswordService;
import com.healthshield.service.RefreshTokenService;
import com.healthshield.util.JwtUtil;
import com.healthshield.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:62486")
public class AuthController {

    private final AuthService authService;

    // ── NEW injections ──
    private final ForgotPasswordService forgotPasswordService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final com.healthshield.service.N8nEmailService n8nEmailService;

    // ── existing endpoints (unchanged) ──
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return new ResponseEntity<>(authService.register(request), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Temporary test endpoint for n8n email
    @GetMapping("/test-email")
    public String testEmail() {
        n8nEmailService.sendEmail(
            "jayaprakashpuntikura@gmail.com",
            "InsureWell n8n Test",
            "Email via n8n is working!"
        );
        return "Email sent!";
    }

    // ── NEW: Refresh token endpoint ──
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            String email = refreshTokenService.getEmailFromToken(refreshToken);
            String newRefreshToken = refreshTokenService.validateAndRotate(refreshToken);

            UserDetails userDetails = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String newAccessToken = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── NEW: Forgot password — Step 1: Send OTP ──
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            forgotPasswordService.sendOtp(body.get("email"));
            return ResponseEntity.ok(Map.of("message",
                    "OTP sent to your registered email address"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── NEW: Forgot password — Step 2: Verify OTP + reset password ──
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            forgotPasswordService.resetPassword(
                    body.get("email"),
                    body.get("otp"),
                    body.get("newPassword")
            );
            return ResponseEntity.ok(Map.of("message",
                    "Password reset successfully. Please log in with your new password."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}