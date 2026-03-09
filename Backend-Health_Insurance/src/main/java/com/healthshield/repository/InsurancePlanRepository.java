package com.healthshield.repository;

import com.healthshield.entity.InsurancePlan;
import com.healthshield.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsurancePlanRepository extends JpaRepository<InsurancePlan, Long> {
    List<InsurancePlan> findByIsActiveTrue();
    List<InsurancePlan> findByPlanTypeAndIsActiveTrue(PlanType planType);
}

