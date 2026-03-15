package com.healthshield.repository;

import com.healthshield.entity.PolicyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyMemberRepository extends JpaRepository<PolicyMember, Long> {
    List<PolicyMember> findByPolicyPolicyId(Long policyId);
    void deleteByPolicyPolicyId(Long policyId);
}

