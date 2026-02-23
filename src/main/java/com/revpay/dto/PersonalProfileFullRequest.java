package com.revpay.dto;

import com.revpay.enums.BankAccountType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonalProfileFullRequest {

    // User
    private String username;

    // Personal Profile
    private LocalDate dob;
    private String address;

    // Bank Account
    private String accountHolderName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private BankAccountType accountType;
    private Boolean isPrimary = true;
}