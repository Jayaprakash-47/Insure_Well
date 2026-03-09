package com.healthshield.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRenewalRequest {

    /** Optional: new nominee name (can update on renewal) */
    private String nomineeName;

    /** Optional: new nominee relationship */
    private String nomineeRelationship;

    /** Optional: linked quote for calculated renewal premium */
    private Long quoteId;
}
