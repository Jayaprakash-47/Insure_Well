package com.healthshield.service;
import com.healthshield.entity.PasswordResetOtp;
import com.healthshield.repository.PasswordResetOtpRepository;
import com.healthshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final PasswordResetOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public void sendOtp(String email) {
        // Verify user exists (works for all roles — single table)
        userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address"));

        // Remove any existing OTPs for this email
        otpRepository.deleteByEmail(email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        PasswordResetOtp entity = PasswordResetOtp.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        otpRepository.save(entity);

        // Send async email
        emailService.sendOtpEmail(email, otp);
        log.info("OTP generated for {}", email);
    }

    public void resetPassword(String email, String otp, String newPassword) {
        PasswordResetOtp entity = otpRepository.findByEmailAndUsedFalse(email)
                .orElseThrow(() -> new RuntimeException("OTP not found or already used"));

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }
        if (!entity.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP. Please check and try again.");
        }

        // Mark OTP as used
        entity.setUsed(true);
        otpRepository.save(entity);

        // Update password — works for Admin, ClaimsOfficer, Underwriter, Customer
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successful for {}", email);
    }
}