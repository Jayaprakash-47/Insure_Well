package com.healthshield.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnderwriterQuoteRequest {

    @NotNull(message = "Quote amount is required")
    private BigDecimal quoteAmount;

    private String remarks;
}
