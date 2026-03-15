package com.healthshield.service;

import com.healthshield.dto.ChangePasswordRequest;
import com.healthshield.dto.ProfileRequest;
import com.healthshield.dto.ProfileResponse;
import com.healthshield.entity.User;
import com.healthshield.repository.UserRepository;
import jakarta.persistence.DiscriminatorValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    // ── Helper: derive role string from @DiscriminatorValue ──────────────────
    private String getRole(User user) {
        DiscriminatorValue dv = user.getClass().getAnnotation(DiscriminatorValue.class);
        return (dv != null) ? dv.value() : "USER";
    }

    // ── Helper: combine firstName + lastName ─────────────────────────────────
    private String getFullName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last  = user.getLastName()  != null ? user.getLastName()  : "";
        return (first + " " + last).trim();
    }

    /**
     * Fetch the profile of the currently logged-in user.
     */
    public ProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    /**
     * Update firstName, lastName, phone, address, city, state, pincode.
     * Email is NOT updatable (it is the login identifier).
     */
    @Transactional
    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Split the incoming "name" field into firstName / lastName
        if (request.getName() != null && !request.getName().isBlank()) {
            String[] parts = request.getName().trim().split("\\s+", 2);
            user.setFirstName(parts[0]);
            user.setLastName(parts.length > 1 ? parts[1] : "");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getState() != null) {
            user.setState(request.getState());
        }
        if (request.getPincode() != null) {
            user.setPincode(request.getPincode());
        }

        User saved = userRepository.save(user);

        // Correct signature: log(action, performedBy, userEmail, details)
        auditLogService.log(
                "PROFILE_UPDATED",
                getRole(user),
                email,
                "User " + email + " updated their profile"
        );

        return mapToResponse(saved);
    }

    /**
     * Change password:
     * 1. Verify current password matches the stored hash.
     * 2. Confirm new passwords match each other.
     * 3. Prevent reuse of the current password.
     * 4. Encode and save the new password.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Correct signature: log(action, performedBy, userEmail, details)
        auditLogService.log(
                "PASSWORD_CHANGED",
                getRole(user),
                email,
                "User " + email + " changed their password"
        );
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private ProfileResponse mapToResponse(User user) {
        ProfileResponse resp = new ProfileResponse();
        resp.setUserId(user.getUserId());
        resp.setName(getFullName(user));
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setRole(getRole(user));
        resp.setAddress(user.getAddress());
        resp.setCity(user.getCity());
        resp.setState(user.getState());
        resp.setPincode(user.getPincode());

        if (user.getCreatedAt() != null) {
            resp.setCreatedAt(
                    user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            );
        }
        return resp;
    }
}