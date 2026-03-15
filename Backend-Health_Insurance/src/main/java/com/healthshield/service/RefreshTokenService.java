package com.healthshield.service;

import com.healthshield.entity.RefreshToken;
import com.healthshield.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    public String createRefreshToken(String email) {
        // Revoke all existing tokens for this user first
        refreshTokenRepository.revokeAllByUserEmail(email);

        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userEmail(email)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .build();
        refreshTokenRepository.save(token);
        return token.getToken();
    }

    public String validateAndRotate(String tokenValue) {
        RefreshToken token = refreshTokenRepository
                .findByTokenAndRevokedFalse(tokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid or revoked refresh token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new RuntimeException("Refresh token has expired. Please log in again.");
        }

        // Rotate: revoke old, create new
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return createRefreshToken(token.getUserEmail());
    }

    public String getEmailFromToken(String tokenValue) {
        return refreshTokenRepository
                .findByTokenAndRevokedFalse(tokenValue)
                .map(RefreshToken::getUserEmail)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }
}