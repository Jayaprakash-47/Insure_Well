package com.healthshield.repository;

import com.healthshield.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserUserId(Long userId);
    List<Payment> findByPolicyPolicyId(Long policyId);
}

