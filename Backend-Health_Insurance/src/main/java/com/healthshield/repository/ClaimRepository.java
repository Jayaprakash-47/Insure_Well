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

    long countByAssignedOfficerUserIdAndClaimStatus(Long officerId, ClaimStatus status);
    long countByAssignedOfficerIsNullAndClaimStatus(ClaimStatus status);
    long countByAssignedOfficerUserIdAndIsEscalatedTrue(Long officerId);

    /** Check if claim number exists */
    boolean existsByClaimNumber(String claimNumber);

    /** Find claim by ID with all related entities eagerly loaded */
    @Query("SELECT c FROM Claim c " +
           "LEFT JOIN FETCH c.policy p " +
           "LEFT JOIN FETCH p.plan " +
           "LEFT JOIN FETCH c.user u " +
           "LEFT JOIN FETCH c.assignedOfficer o " +
           "LEFT JOIN FETCH c.documents " +
           "WHERE c.claimId = :claimId")
    Claim findByIdWithDetails(@Param("claimId") Long claimId);

}
