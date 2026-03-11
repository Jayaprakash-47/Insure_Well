package com.healthshield.enums;

public enum ClaimStatus {
    SUBMITTED,           // Customer just filed the claim
    UNDER_REVIEW,        // Claims Officer picked it up
    DOCUMENT_PENDING,    // Officer requested additional documents
    APPROVED,            // Fully approved
    PARTIALLY_APPROVED,  // Approved but with reduced amount
    REJECTED,            // Rejected with reason
    SETTLEMENT_IN_PROGRESS, // Approved and payment being processed
    SETTLED,             // Final — money disbursed to customer/hospital
    APPEAL_REQUESTED     // Customer appealed a rejection
}
