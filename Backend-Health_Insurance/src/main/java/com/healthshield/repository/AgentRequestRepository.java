package com.healthshield.repository;

import com.healthshield.entity.AgentRequest;
import com.healthshield.enums.AgentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRequestRepository extends JpaRepository<AgentRequest, Long> {

    // All requests by a specific customer
    List<AgentRequest> findByCustomerUserIdOrderByCreatedAtDesc(Long customerId);

    // All PENDING requests (for underwriters to pick up)
    List<AgentRequest> findByStatusOrderByCreatedAtAsc(AgentRequestStatus status);

    // Requests accepted by a specific underwriter
    List<AgentRequest> findByAssignedUnderwriterUserIdOrderByCreatedAtDesc(Long underwriterId);
}