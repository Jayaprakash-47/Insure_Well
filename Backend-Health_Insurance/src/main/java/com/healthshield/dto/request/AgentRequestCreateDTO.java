package com.healthshield.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AgentRequestCreateDTO {

    @NotBlank(message = "Plan type interest is required")
    private String planTypeInterest;   // INDIVIDUAL / FAMILY / SENIOR_CITIZEN

    private String preferredTime;       // e.g. "Tomorrow 10 AM", "Weekends"

    private String message;             // Any additional info

    public String getPlanTypeInterest() { return planTypeInterest; }
    public void setPlanTypeInterest(String v) { this.planTypeInterest = v; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String v) { this.preferredTime = v; }

    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
}


// ─────────────────────────────────────────────────────────────────────────────
// FILE: com/healthshield/dto/response/AgentRequestResponse.java
// ─────────────────────────────────────────────────────────────────────────────
// (Put in separate file — shown together here for convenience)