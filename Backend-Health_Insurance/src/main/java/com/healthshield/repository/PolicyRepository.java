package com.healthshield.repository;

import com.healthshield.entity.Policy;
import com.healthshield.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByUserUserId(Long userId);
    List<Policy> findByPolicyStatus(PolicyStatus status);
    boolean existsByPolicyNumber(String policyNumber);

    /** Policies assigned to a specific underwriter */
    List<Policy> findByAssignedUnderwriterUserId(Long underwriterId);

    /** Count policies assigned to underwriter */
    long countByAssignedUnderwriterUserId(Long underwriterId);

    /** Pending policies (not yet assigned or quoted) */
    List<Policy> findByPolicyStatusIn(List<PolicyStatus> statuses);

    /** Policies expiring within next N days */
    @Query("SELECT p FROM Policy p WHERE p.policyStatus = 'ACTIVE' AND p.endDate BETWEEN :today AND :futureDate")
    List<Policy> findExpiringPolicies(@Param("today") LocalDate today, @Param("futureDate") LocalDate futureDate);

    /** Policies that are already expired but still marked active */
    @Query("SELECT p FROM Policy p WHERE p.policyStatus = 'ACTIVE' AND p.endDate < :today")
    List<Policy> findExpiredButActive(@Param("today") LocalDate today);

    /** Original policy reference for renewals */
    List<Policy> findByOriginalPolicyPolicyId(Long originalPolicyId);

    Optional<Policy> findByPolicyNumber(String policyNumber);

    @Query("SELECT p FROM Policy p JOIN FETCH p.user WHERE p.policyStatus = :status")
    List<Policy> findActiveWithUsers(@Param("status") PolicyStatus status);
}

