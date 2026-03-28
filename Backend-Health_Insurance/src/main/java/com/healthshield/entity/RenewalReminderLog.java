package com.healthshield.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "renewal_reminder_logs",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"policy_id", "days_before_expiry"}
        )
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RenewalReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "days_before_expiry", nullable = false)
    private Integer daysBeforeExpiry;   // 30 | 15 | 7

    @Column(nullable = false)
    private LocalDateTime sentAt;
}