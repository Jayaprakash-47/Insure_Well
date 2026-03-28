package com.healthshield.repository;

import com.healthshield.entity.Policy;
import com.healthshield.entity.RenewalReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenewalReminderLogRepository
        extends JpaRepository<RenewalReminderLog, Long> {

    boolean existsByPolicyAndDaysBeforeExpiry(Policy policy, Integer days);
}