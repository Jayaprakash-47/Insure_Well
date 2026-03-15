package com.healthshield.repository;
import com.healthshield.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByEmailAndUsedFalse(String email);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}