package com.healthshield.repository;

import com.healthshield.entity.ClaimsOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimsOfficerRepository extends JpaRepository<ClaimsOfficer, Long> {
    Optional<ClaimsOfficer> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}
