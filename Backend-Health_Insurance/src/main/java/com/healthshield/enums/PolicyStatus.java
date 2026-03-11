package com.healthshield.enums;

public enum PolicyStatus {
    PENDING,         // Customer submitted a policy application, awaiting underwriter assignment
    ASSIGNED,        // Admin has assigned an underwriter to evaluate the application
    QUOTE_SENT,      // Underwriter has calculated and sent premium quote to customer
    ACTIVE,          // Customer paid the premium, policy is live
    EXPIRED,         // Policy term ended
    CANCELLED,       // Customer or admin cancelled
    LAPSED,          // Premium not paid on time
    RENEWAL_DUE,     // Policy nearing expiry, renewal pending
    RENEWED          // Successfully renewed for another term
}
