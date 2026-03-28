package com.healthshield.controller;

import com.healthshield.dto.ChangePasswordRequest;
import com.healthshield.dto.ProfileRequest;
import com.healthshield.dto.ProfileResponse;
import com.healthshield.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * GET /api/profile
     * Returns the profile of the currently authenticated user.
     * Accessible by ALL roles.
     */
    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        ProfileResponse profile = profileService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/profile
     * Updates name, phone, address, city, state, pincode.
     * Email is NOT updatable.
     */
    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProfileRequest request) {
        ProfileResponse updated = profileService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/profile/change-password
     * Verifies current password, then updates to new password.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // =================== AADHAAR SANDBOX VERIFICATION ===================
    @PostMapping(value = "/verify-aadhaar-sandbox", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> verifyAadhaarSandbox(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        profileService.verifyAadhaarSandbox(userDetails.getUsername(), aadhaarNumber, file);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Aadhaar verified successfully via Sandbox API",
                "kycStatus", "VERIFIED"
        ));
    }
}