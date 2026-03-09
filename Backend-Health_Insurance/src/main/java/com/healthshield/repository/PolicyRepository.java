package com.healthshield.repository;

import com.healthshield.entity.Policy;
import com.healthshield.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByUserUserId(Long userId);
    List<Policy> findByPolicyStatus(PolicyStatus status);
    boolean existsByPolicyNumber(String policyNumber);

    /** Policies sold by a specific agent */
    List<Policy> findBySoldByAgentUserId(Long agentId);

    /** Count policies sold by agent */
    long countBySoldByAgentUserId(Long agentId);

    /** Policies expiring within next N days */
    @Query("SELECT p FROM Policy p WHERE p.policyStatus = 'ACTIVE' AND p.endDate BETWEEN :today AND :futureDate")
    List<Policy> findExpiringPolicies(@Param("today") LocalDate today, @Param("futureDate") LocalDate futureDate);

    /** Policies that are already expired but still marked active */
    @Query("SELECT p FROM Policy p WHERE p.policyStatus = 'ACTIVE' AND p.endDate < :today")
    List<Policy> findExpiredButActive(@Param("today") LocalDate today);

    /** Policies sold by agent with specific status */
    List<Policy> findBySoldByAgentUserIdAndPolicyStatus(Long agentId, PolicyStatus status);

    /** Original policy reference for renewals */
    List<Policy> findByOriginalPolicyPolicyId(Long originalPolicyId);
}
