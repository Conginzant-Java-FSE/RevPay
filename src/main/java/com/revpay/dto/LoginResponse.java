package com.revpay.dto;

import com.revpay.enums.AccountType;

public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String fullName;
    private String email;
    private AccountType accountType;
    private String message;

    public LoginResponse(String token, Long userId, String fullName, String email, AccountType accountType) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.accountType = accountType;
        this.message = "Login successful";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
