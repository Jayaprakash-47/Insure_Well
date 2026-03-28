package com.healthshield.enums;

public enum KycStatus {
    PENDING,    // Aadhaar uploaded, not yet reviewed
    VERIFIED,   // Underwriter verified the Aadhaar
    REJECTED    // Underwriter rejected — customer must re-upload
}