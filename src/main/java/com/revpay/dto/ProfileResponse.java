package com.revpay.dto;

import com.revpay.enums.AccountType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileResponse {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private AccountType accountType;

    public ProfileResponse(Long userId, String username, String fullName,
                           String email, String phone, AccountType accountType) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountType = accountType;
    }

    public ProfileResponse() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public AccountType getAccountType() {
        return accountType;
    }
}