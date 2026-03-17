package com.revpay.dto;

public class ResendOtpRequest {
    private String emailOrPhone;

    public ResendOtpRequest() {}

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }
}
