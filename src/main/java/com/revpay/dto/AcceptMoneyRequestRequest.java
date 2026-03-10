package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcceptMoneyRequestRequest {

    @NotBlank(message = "Transaction PIN is required")
    private String pin;
}