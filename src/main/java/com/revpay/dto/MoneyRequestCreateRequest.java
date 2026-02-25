package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MoneyRequestCreateRequest {

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}
