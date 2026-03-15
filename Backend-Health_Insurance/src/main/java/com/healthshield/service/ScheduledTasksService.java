package com.healthshield.service;

import com.healthshield.entity.Policy;
import com.healthshield.enums.NotificationType;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.repository.PasswordResetOtpRepository;
import com.healthshield.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PasswordResetOtpRepository otpRepository;
    private final PolicyRepository policyRepository;

    // ── Runs every day at midnight ──
    @Scheduled(cron = "0 0 0 * * *")
    public void autoExpirePolicies() {
        log.info("Running policy auto-expiry job...");

        // Uses your existing query: finds ACTIVE policies where endDate < today
        List<Policy> expired = policyRepository.findExpiredButActive(LocalDate.now());

        for (Policy policy : expired) {
            policy.setPolicyStatus(PolicyStatus.EXPIRED);
            policyRepository.save(policy);

            String email     = policy.getUser().getEmail();
            String firstName = policy.getUser().getFirstName();
            String policyNo  = policy.getPolicyNumber();

            notificationService.sendNotification(
                    email,
                    "Your policy " + policyNo + " has expired. Please renew to continue coverage.",
                    NotificationType.POLICY_EXPIRED
            );
            emailService.sendStatusChangeEmail(email, firstName, policyNo, "EXPIRED");
        }

        log.info("Auto-expiry job completed — expired {} policies", expired.size());
    }

    // ── Runs every day at 8 AM — 30-day renewal reminder ──
    @Scheduled(cron = "0 0 8 * * *")
    public void sendRenewalReminders() {
        log.info("Running renewal reminder job...");

        // Uses your existing query: finds ACTIVE policies expiring in next 30 days
        LocalDate today      = LocalDate.now();
        LocalDate thirtyDays = today.plusDays(30);

        List<Policy> dueSoon = policyRepository.findExpiringPolicies(today, thirtyDays);

        for (Policy policy : dueSoon) {
            String email     = policy.getUser().getEmail();
            String firstName = policy.getUser().getFirstName();
            String policyNo  = policy.getPolicyNumber();

            emailService.sendRenewalReminderEmail(
                    email, firstName, policyNo, policy.getEndDate());

            notificationService.sendNotification(
                    email,
                    "Your policy " + policyNo + " expires in 30 days. Renew now to avoid a lapse.",
                    NotificationType.RENEWAL_DUE
            );
        }

        log.info("Renewal reminder job completed — reminded {} customers", dueSoon.size());
    }

    // ── Runs every 15 minutes — cleans up expired OTPs ──
    @Scheduled(fixedRate = 900_000)
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Cleaned up expired OTPs");
    }
}