package com.revpay.dto;

import com.revpay.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private AccountType accountType;
    //private String securityQuestion;

}