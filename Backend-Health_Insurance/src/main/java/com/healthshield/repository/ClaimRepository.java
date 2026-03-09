package com.healthshield.repository;

import com.healthshield.entity.Claim;
import com.healthshield.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByUserUserId(Long userId);
    List<Claim> findByClaimStatus(ClaimStatus status);
    List<Claim> findByPolicyPolicyId(Long policyId);

    /** Claims assigned to a specific officer */
    List<Claim> findByAssignedOfficerUserId(Long officerId);

    /** Claims assigned to an officer with specific status */
    List<Claim> findByAssignedOfficerUserIdAndClaimStatus(Long officerId, ClaimStatus status);

    /** Unassigned claims (no officer assigned yet) */
    List<Claim> findByAssignedOfficerIsNullAndClaimStatus(ClaimStatus status);

    /** Escalated claims for admin review */
    List<Claim> findByIsEscalatedTrue();

    /** Escalated and unresolved claims */
    List<Claim> findByIsEscalatedTrueAndEscalationResolvedByIsNull();

    /** Claims reviewed by a specific officer (completed reviews) */
    List<Claim> findByAssignedOfficerUserIdAndReviewedAtIsNotNull(Long officerId);

    /** Count claims by status */
    long countByClaimStatus(ClaimStatus status);

    /** Count claims assigned to officer */
    long countByAssignedOfficerUserId(Long officerId);

    /** All claims for policies sold by a specific agent */
    @Query("SELECT c FROM Claim c WHERE c.policy.soldByAgent.userId = :agentId")
    List<Claim> findByAgentId(@Param("agentId") Long agentId);
}
