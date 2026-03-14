package com.revpay.dto;

public class TwoFactorResponse {
    private boolean requiresOtp;
    private String message;
    private String token;

    public TwoFactorResponse(boolean requiresOtp, String message, String token) {
        this.requiresOtp = requiresOtp;
        this.message = message;
        this.token = token;
    }

    public TwoFactorResponse() {}

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
