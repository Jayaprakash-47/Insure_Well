package com.healthshield.service;

import com.healthshield.dto.request.PremiumCalculateRequest;
import com.healthshield.dto.response.PremiumQuoteResponse;
import com.healthshield.entity.InsurancePlan;
import com.healthshield.entity.PremiumQuote;
import com.healthshield.entity.User;
import com.healthshield.exception.ResourceNotFoundException;
import com.healthshield.repository.InsurancePlanRepository;
import com.healthshield.repository.PremiumQuoteRepository;
import com.healthshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PremiumQuoteService {

    private final PremiumQuoteRepository premiumQuoteRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final UserRepository userRepository;

    public PremiumQuoteResponse calculatePremium(Long userId, PremiumCalculateRequest request) {
        InsurancePlan plan = insurancePlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Insurance plan not found with id: " + request.getPlanId()));

        int age = request.getAge();
        boolean smoker = request.getSmoker() != null && request.getSmoker();
        boolean preExisting = request.getPreExistingDiseases() != null && request.getPreExistingDiseases();
        int members = (request.getNumberOfMembers() != null && request.getNumberOfMembers() > 0)
                ? request.getNumberOfMembers() : 1;

        // Age factor
        double ageFactor;
        if (age <= 30) ageFactor = 1.0;
        else if (age <= 40) ageFactor = 1.2;
        else if (age <= 50) ageFactor = 1.5;
        else if (age <= 60) ageFactor = 1.8;
        else ageFactor = 2.2;

        // Smoker factor
        double smokerFactor = smoker ? 1.2 : 1.0;

        // Disease factor
        double diseaseFactor = preExisting ? 1.3 : 1.0;

        // Member factor
        double memberFactor = 1.0 + (members - 1) * 0.7;

        BigDecimal calculatedPremium = plan.getBasePremiumAmount()
                .multiply(BigDecimal.valueOf(ageFactor))
                .multiply(BigDecimal.valueOf(smokerFactor))
                .multiply(BigDecimal.valueOf(diseaseFactor))
                .multiply(BigDecimal.valueOf(memberFactor))
                .setScale(2, RoundingMode.HALF_UP);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        PremiumQuote quote = PremiumQuote.builder()
                .user(user)
                .plan(plan)
                .age(age)
                .smoker(smoker)
                .preExistingDiseases(preExisting)
                .numberOfMembers(members)
                .calculatedPremium(calculatedPremium)
                .build();

        PremiumQuote saved = premiumQuoteRepository.save(quote);

        return mapToResponse(saved, plan.getPlanName());
    }

    public List<PremiumQuoteResponse> getQuotesByUser(Long userId) {
        return premiumQuoteRepository.findByUserUserId(userId).stream()
                .map(q -> mapToResponse(q, q.getPlan().getPlanName()))
                .collect(Collectors.toList());
    }

    private PremiumQuoteResponse mapToResponse(PremiumQuote quote, String planName) {
        return PremiumQuoteResponse.builder()
                .quoteId(quote.getQuoteId())
                .planName(planName)
                .age(quote.getAge())
                .smoker(quote.getSmoker())
                .preExistingDiseases(quote.getPreExistingDiseases())
                .numberOfMembers(quote.getNumberOfMembers())
                .calculatedPremium(quote.getCalculatedPremium())
                .calculatedAt(quote.getCalculatedAt())
                .build();
    }
}

