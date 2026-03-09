package com.healthshield.repository;

import com.healthshield.entity.PremiumQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PremiumQuoteRepository extends JpaRepository<PremiumQuote, Long> {
    List<PremiumQuote> findByUserUserId(Long userId);
}

