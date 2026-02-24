package com.revpay.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddFundsRequest {

    @NotBlank(message = "Card ID is required")
    private String cardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is 1.00")
    @DecimalMax(value = "10000.00", message = "Maximum amount is 10,000.00")
    private BigDecimal amount;

    @NotBlank(message = "Transaction PIN is required")
    private String pin;
}