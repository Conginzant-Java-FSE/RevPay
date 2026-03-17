package com.revpay.dto;

public class VerifyOtpRequest {
    private String emailOrPhone;
    private String otp;

    public VerifyOtpRequest() {}

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
