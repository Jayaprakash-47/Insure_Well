package com.healthshield.scheduler;

import com.healthshield.entity.Policy;
import com.healthshield.entity.RenewalReminderLog;
import com.healthshield.enums.NotificationType;
import com.healthshield.enums.PolicyStatus;
import com.healthshield.repository.PolicyRepository;
import com.healthshield.repository.RenewalReminderLogRepository;
import com.healthshield.service.EmailService;
import com.healthshield.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyRenewalScheduler {

    private final PolicyRepository             policyRepo;
    private final RenewalReminderLogRepository logRepo;
    private final EmailService                 emailService;
    private final NotificationService          notificationService;

    private static final int[] THRESHOLDS = {30, 15, 7};

    /** Runs every day at 9:00 AM */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkRenewals() {
        log.info("Renewal reminder scheduler started");
        LocalDate today = LocalDate.now();

        List<Policy> active = policyRepo.findByPolicyStatus(PolicyStatus.ACTIVE);

        for (Policy policy : active) {
            if (policy.getEndDate() == null) continue;

            long daysLeft = today.until(policy.getEndDate(), ChronoUnit.DAYS);

            for (int threshold : THRESHOLDS) {
                if (daysLeft == threshold) {
                    sendReminder(policy, threshold);
                }
            }
        }
        log.info("Renewal reminder scheduler done — {} active policies checked",
                active.size());
    }

    private void sendReminder(Policy policy, int daysLeft) {
        // ── Deduplication — never send the same reminder twice ──────────
        if (logRepo.existsByPolicyAndDaysBeforeExpiry(policy, daysLeft)) {
            log.debug("Skipping {}d reminder for {} — already sent",
                    daysLeft, policy.getPolicyNumber());
            return;
        }

        String email = policy.getUser().getEmail();
        String name  = policy.getUser().getFirstName()
                + " " + policy.getUser().getLastName();

        // ── Email (your existing method) ────────────────────────────────
        emailService.sendRenewalReminderEmail(
                email, name, policy.getPolicyNumber(), policy.getEndDate());

        // ── In-app SSE push (your existing method) ──────────────────────
        String message = "Your policy " + policy.getPolicyNumber()
                + " expires in " + daysLeft + " day"
                + (daysLeft == 1 ? "" : "s")
                + " on " + policy.getEndDate()
                + ". Renew now to stay covered.";

        notificationService.sendNotification(email, message, NotificationType.RENEWAL_DUE);

        // ── Log so it never fires again for this policy+threshold ───────
        logRepo.save(RenewalReminderLog.builder()
                .policy(policy)
                .daysBeforeExpiry(daysLeft)
                .sentAt(LocalDateTime.now())
                .build());

        log.info("Sent {}d renewal reminder → {} ({})",
                daysLeft, email, policy.getPolicyNumber());
    }
}