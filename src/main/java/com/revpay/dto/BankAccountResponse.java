package com.revpay.dto;

import com.revpay.enums.BankAccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BankAccountResponse {

    private String bankName;
    private String accountNumber; // masked
    private BankAccountType accountType;
}
