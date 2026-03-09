package com.healthshield.controller;

import com.healthshield.dto.request.InsurancePlanRequest;
import com.healthshield.dto.response.InsurancePlanResponse;
import com.healthshield.service.InsurancePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class InsurancePlanController {

    private final InsurancePlanService insurancePlanService;

    @GetMapping
    public ResponseEntity<List<InsurancePlanResponse>> getAllPlans() {
        return ResponseEntity.ok(insurancePlanService.getAllActivePlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsurancePlanResponse> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(insurancePlanService.getPlanById(id));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<InsurancePlanResponse>> getPlansByType(@PathVariable String type) {
        return ResponseEntity.ok(insurancePlanService.getPlansByType(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsurancePlanResponse> createPlan(@Valid @RequestBody InsurancePlanRequest request) {
        return new ResponseEntity<>(insurancePlanService.createPlan(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InsurancePlanResponse> updatePlan(@PathVariable Long id,
                                                            @Valid @RequestBody InsurancePlanRequest request) {
        return ResponseEntity.ok(insurancePlanService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivatePlan(@PathVariable Long id) {
        insurancePlanService.deactivatePlan(id);
        return ResponseEntity.noContent().build();
    }
}

