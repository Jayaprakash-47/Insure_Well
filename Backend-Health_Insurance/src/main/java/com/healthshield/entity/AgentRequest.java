package com.healthshield.entity;
import com.healthshield.enums.AgentRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;



@Entity
@Table(name = "agent_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The customer who raised this request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** The underwriter who accepted and will process this request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter assignedUnderwriter;

    /** What type of plan the customer is interested in */
    private String planTypeInterest;

    /** Customer's preferred time slot (free text) */
    private String preferredTime;

    /** Any additional message from the customer */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Current status of the request */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AgentRequestStatus status = AgentRequestStatus.PENDING;

    /** The policy created by the underwriter on behalf of the customer */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy resultingPolicy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
}