package com.revpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendMoneyRequest {

    @NotBlank(message = "Receiver email or phone is required")
    private String receiverEmailOrPhone;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Transaction PIN is required")
    private String pin;

    private String note;
}