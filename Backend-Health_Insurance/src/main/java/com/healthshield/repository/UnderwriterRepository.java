package com.healthshield.repository;

import com.healthshield.entity.Underwriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnderwriterRepository extends JpaRepository<Underwriter, Long> {
}
