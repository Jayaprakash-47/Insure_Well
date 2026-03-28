package com.healthshield.entity;

import com.healthshield.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("CUSTOMER")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Customer extends User {

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // Bank Details
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
    private String bankName;

    // KYC Details
    private String aadhaarNumber;
    private Boolean aadhaarVerified = false;
}

