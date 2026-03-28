package com.healthshield.enums;



public enum AgentRequestStatus {
    PENDING,    // Customer raised, waiting for underwriter
    ACCEPTED,   // Underwriter accepted, processing
    COMPLETED,  // Policy submitted successfully
    CANCELLED   // Customer or underwriter cancelled
}