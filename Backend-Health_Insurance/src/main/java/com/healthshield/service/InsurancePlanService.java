package com.healthshield.service;

import com.healthshield.dto.request.InsurancePlanRequest;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.entity.InsurancePlan;
import com.healthshield.enums.PlanType;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.repository.InsurancePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsurancePlanService {

    private final InsurancePlanRepository insurancePlanRepository;

    public List<InsurancePlanResponse> getAllActivePlans() {
        return insurancePlanRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InsurancePlanResponse getPlanById(Long id) {
        InsurancePlan plan = insurancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + id));
        return mapToResponse(plan);
    }

    public List<InsurancePlanResponse> getPlansByType(String type) {
        PlanType planType = PlanType.valueOf(type.toUpperCase());
        return insurancePlanRepository.findByPlanTypeAndIsActiveTrue(planType).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InsurancePlanResponse createPlan(InsurancePlanRequest request) {
        InsurancePlan plan = InsurancePlan.builder()
                .planName(request.getPlanName())
                .planType(PlanType.valueOf(request.getPlanType().toUpperCase()))
                .description(request.getDescription())
                .basePremiumAmount(request.getBasePremiumAmount())
                .coverageAmount(request.getCoverageAmount())
                .planDurationMonths(request.getPlanDurationMonths())
                .minAgeLimit(request.getMinAgeLimit())
                .maxAgeLimit(request.getMaxAgeLimit())
                .waitingPeriodMonths(request.getWaitingPeriodMonths())
                .maternityCover(request.getMaternityCover() != null ? request.getMaternityCover() : false)
                .preExistingDiseaseCover(request.getPreExistingDiseaseCover() != null ? request.getPreExistingDiseaseCover() : false)
                .isActive(true)
                .build();

        InsurancePlan saved = insurancePlanRepository.save(plan);
        return mapToResponse(saved);
    }

    public InsurancePlanResponse updatePlan(Long id, InsurancePlanRequest request) {
        InsurancePlan plan = insurancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + id));

        plan.setPlanName(request.getPlanName());
        plan.setPlanType(PlanType.valueOf(request.getPlanType().toUpperCase()));
        plan.setDescription(request.getDescription());
        plan.setBasePremiumAmount(request.getBasePremiumAmount());
        plan.setCoverageAmount(request.getCoverageAmount());
        plan.setPlanDurationMonths(request.getPlanDurationMonths());
        plan.setMinAgeLimit(request.getMinAgeLimit());
        plan.setMaxAgeLimit(request.getMaxAgeLimit());
        plan.setWaitingPeriodMonths(request.getWaitingPeriodMonths());
        plan.setMaternityCover(request.getMaternityCover());
        plan.setPreExistingDiseaseCover(request.getPreExistingDiseaseCover());

        InsurancePlan saved = insurancePlanRepository.save(plan);
        return mapToResponse(saved);
    }

    public void deactivatePlan(Long id) {
        InsurancePlan plan = insurancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + id));
        plan.setIsActive(false);
        insurancePlanRepository.save(plan);
    }

    public void activatePlan(Long id) {
        InsurancePlan plan = insurancePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + id));
        plan.setIsActive(true);
        insurancePlanRepository.save(plan);
    }

    private InsurancePlanResponse mapToResponse(InsurancePlan plan) {
        return InsurancePlanResponse.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .planType(plan.getPlanType().name())
                .description(plan.getDescription())
                .basePremiumAmount(plan.getBasePremiumAmount())
                .coverageAmount(plan.getCoverageAmount())
                .planDurationMonths(plan.getPlanDurationMonths())
                .minAgeLimit(plan.getMinAgeLimit())
                .maxAgeLimit(plan.getMaxAgeLimit())
                .waitingPeriodMonths(plan.getWaitingPeriodMonths())
                .maternityCover(plan.getMaternityCover())
                .preExistingDiseaseCover(plan.getPreExistingDiseaseCover())
                .isActive(plan.getIsActive())
                .build();
    }
}

