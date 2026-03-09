package com.healthshield.enums;

public enum PolicyStatus {
    PENDING,         // Policy purchased but not yet paid
    ACTIVE,          // Premium paid, policy is live
    EXPIRED,         // Policy term ended
    CANCELLED,       // Customer or admin cancelled
    LAPSED,          // Premium not paid on time
    RENEWAL_DUE,     // Policy nearing expiry, renewal pending
    RENEWED          // Successfully renewed for another term
}
