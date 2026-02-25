package com.revpay.dto;

import com.revpay.enums.BankAccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBankAccountRequest {

    @NotBlank(message = "Bank name is required")
    private String bankName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String ifscCode;

    @NotNull(message = "Account type is required")
    private BankAccountType accountType;

    @NotBlank(message = "Password is required to update bank account details")
    private String password;
}