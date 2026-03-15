package com.healthshield.config;

import com.healthshield.entity.*;
import com.healthshield.enums.Gender;
import com.healthshield.enums.PlanType;
import com.healthshield.repository.InsurancePlanRepository;
import com.healthshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final InsurancePlanRepository insurancePlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedInsurancePlans();

        log.info("═══════════════════════════════════════════════════════════");
        log.info("  🏥 HealthShield Insurance System — Started Successfully!");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  Swagger UI : http://localhost:8080/swagger-ui.html");
        log.info("  H2 Console : http://localhost:8080/h2-console");
        log.info("  API Docs   : http://localhost:8080/api-docs");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  DEFAULT CREDENTIALS:");
        log.info("  Admin           : admin@healthshield.com / Admin@1234");
        log.info("  Claims Officer  : ravi.co@healthshield.com / Officer@1234");
         log.info("  Under Writer    : priya.underwriter@healthshield.com / Agent@1234");
        log.info("  Customer        : jayaprakashpuntikura@gmail.com / Jaya@1234");
        log.info("═══════════════════════════════════════════════════════════");
    }

    private void seedUsers() {
        // Seed Admin
        if (!userRepository.existsByEmail("admin@healthshield.com")) {
            Admin admin = new Admin();
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@healthshield.com");
            admin.setPassword(passwordEncoder.encode("Admin@1234"));
            admin.setPhone("9000000001");
            admin.setIsActive(true);
            admin.setCanApproveClaims(true);
            userRepository.save(admin);
            log.info("✅ Admin user seeded: admin@healthshield.com");
        }

        // Seed Claims Officer
        if (!userRepository.existsByEmail("ravi.co@healthshield.com")) {
            ClaimsOfficer officer = new ClaimsOfficer();
            officer.setFirstName("Ravi");
            officer.setLastName("Sharma");
            officer.setEmail("ravi.co@healthshield.com");
            officer.setPassword(passwordEncoder.encode("Officer@1234"));
            officer.setPhone("9000000004");
            officer.setIsActive(true);
            officer.setEmployeeId("EMP-CO-001");
            officer.setDepartment("Claims Processing");
            officer.setSpecialization("General & Cardiology");
            officer.setTotalClaimsProcessed(0);
            officer.setTotalClaimsApproved(0);
            officer.setTotalClaimsRejected(0);
            officer.setApprovalLimit(new BigDecimal("500000.00"));
            userRepository.save(officer);
            log.info("✅ Claims Officer seeded: ravi.co@healthshield.com");
        }

        // Seed Underwriter
        if (!userRepository.existsByEmail("priya.underwriter@healthshield.com")) {
            Underwriter underwriter = new Underwriter();
            underwriter.setFirstName("Priya");
            underwriter.setLastName("Mehta");
            underwriter.setEmail("priya.underwriter@healthshield.com");
            underwriter.setPassword(passwordEncoder.encode("Agent@1234"));
            underwriter.setPhone("9000000002");
            underwriter.setIsActive(true);
            underwriter.setLicenseNumber("LIC-2024-001");
            underwriter.setSpecialization("Health & Life Insurance");
            underwriter.setCommissionPercentage(new BigDecimal("10.00"));
            underwriter.setTotalQuotesSent(0);
            userRepository.save(underwriter);
            log.info("✅ Underwriter seeded: priya.underwriter@healthshield.com");
        }

        // Seed Customer
        if (!userRepository.existsByEmail("jayaprakashpuntikura@gmail.com")) {
            Customer customer = new Customer();
            customer.setFirstName("Jayaprakash");
            customer.setLastName("Kumar");
            customer.setEmail("jayaprakashpuntikura@gmail.com");          // ← updated
            customer.setPassword(passwordEncoder.encode("Jaya@1234"));
            customer.setPhone("9000000003");
            customer.setIsActive(true);
            customer.setDateOfBirth(LocalDate.of(1990, 5, 15));
            customer.setGender(Gender.MALE);
            customer.setAddress("123 Main Street");
            customer.setCity("Chennai");
            customer.setState("Tamil Nadu");
            customer.setPincode("600001");
            userRepository.save(customer);
            log.info("✅ Customer seeded: jayaprakashpuntikura@gmail.com"); // ← updated
        }
    }

    private void seedInsurancePlans() {
        if (insurancePlanRepository.count() == 0) {
            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Basic Health Plan")
                    .planType(PlanType.INDIVIDUAL)
                    .description("Essential health coverage for individuals with basic hospitalization, day-care procedures, and ambulance charges. Ideal for young professionals looking for affordable protection.")
                    .basePremiumAmount(new BigDecimal("5000"))
                    .coverageAmount(new BigDecimal("300000"))
                    .planDurationMonths(12)
                    .minAgeLimit(18)
                    .maxAgeLimit(65)
                    .waitingPeriodMonths(1)
                    .maternityCover(false)
                    .preExistingDiseaseCover(false)
                    .isActive(true)
                    .build());

            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Silver Health Plan")
                    .planType(PlanType.INDIVIDUAL)
                    .description("Enhanced coverage with higher sum insured, cashless treatment at 5000+ network hospitals, day-care procedures, and pre/post hospitalization expenses covered up to 60/90 days.")
                    .basePremiumAmount(new BigDecimal("8500"))
                    .coverageAmount(new BigDecimal("500000"))
                    .planDurationMonths(12)
                    .minAgeLimit(18)
                    .maxAgeLimit(65)
                    .waitingPeriodMonths(2)
                    .maternityCover(false)
                    .preExistingDiseaseCover(false)
                    .isActive(true)
                    .build());

            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Gold Health Plan")
                    .planType(PlanType.INDIVIDUAL)
                    .description("Premium comprehensive coverage including maternity benefits, pre-existing disease cover after waiting period, annual health check-ups, and no room rent capping. Best for complete peace of mind.")
                    .basePremiumAmount(new BigDecimal("15000"))
                    .coverageAmount(new BigDecimal("1000000"))
                    .planDurationMonths(12)
                    .minAgeLimit(18)
                    .maxAgeLimit(65)
                    .waitingPeriodMonths(3)
                    .maternityCover(true)
                    .preExistingDiseaseCover(true)
                    .isActive(true)
                    .build());

            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Family Health Plan")
                    .planType(PlanType.FAMILY)
                    .description("Comprehensive family floater plan covering spouse and up to 3 children. Includes maternity, new-born baby cover, annual health check-ups for all members, and restoration benefit (doubles sum insured if exhausted).")
                    .basePremiumAmount(new BigDecimal("20000"))
                    .coverageAmount(new BigDecimal("1500000"))
                    .planDurationMonths(12)
                    .minAgeLimit(18)
                    .maxAgeLimit(65)
                    .waitingPeriodMonths(2)
                    .maternityCover(true)
                    .preExistingDiseaseCover(true)
                    .isActive(true)
                    .build());

            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Senior Citizen Plan")
                    .planType(PlanType.SENIOR_CITIZEN)
                    .description("Tailored plan for senior citizens aged 60-80 years. Covers pre-existing diseases after 2-year waiting period, domiciliary hospitalization, AYUSH treatment, and dedicated helpline for elderly customers.")
                    .basePremiumAmount(new BigDecimal("25000"))
                    .coverageAmount(new BigDecimal("800000"))
                    .planDurationMonths(12)
                    .minAgeLimit(60)
                    .maxAgeLimit(80)
                    .waitingPeriodMonths(6)
                    .maternityCover(false)
                    .preExistingDiseaseCover(true)
                    .isActive(true)
                    .build());

            insurancePlanRepository.save(InsurancePlan.builder()
                    .planName("Platinum Health Plan")
                    .planType(PlanType.INDIVIDUAL)
                    .description("Ultimate individual plan with maximum ₹20 Lakh coverage, zero waiting period, worldwide emergency coverage, air ambulance, organ donor expenses, and personal accident cover included.")
                    .basePremiumAmount(new BigDecimal("30000"))
                    .coverageAmount(new BigDecimal("2000000"))
                    .planDurationMonths(12)
                    .minAgeLimit(18)
                    .maxAgeLimit(55)
                    .waitingPeriodMonths(0)
                    .maternityCover(true)
                    .preExistingDiseaseCover(true)
                    .isActive(true)
                    .build());

            log.info("✅ 6 Insurance Plans seeded successfully");
        }
    }
}
