package com.revpay.dto;

import com.revpay.enums.AccountType;

public class UserListResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private AccountType accountType;

    public UserListResponse() {
    }

    public UserListResponse(Long userId, String fullName, String email, String phone, AccountType accountType) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountType = accountType;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}