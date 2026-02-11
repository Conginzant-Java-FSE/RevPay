package com.revpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for resetting a forgotten password")
public class ForgotPasswordRequest {

    @NotBlank(message = "Email or phone number is required")
    @Schema(description = "Registered email address or phone number", example = "user@example.com")
    private String emailOrPhone;

    @NotBlank(message = "Security question is required")
    @Schema(description = "The security question set during registration", example = "What is your pet's name?")
    private String securityQuestion;

    @NotBlank(message = "Security answer is required")
    @Schema(description = "Answer to the security question", example = "Buddy")
    private String securityAnswer;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    @Schema(description = "The new password (minimum 8 characters)", example = "NewSecure@123")
    private String newPassword;

    public ForgotPasswordRequest() {
    }

    public ForgotPasswordRequest(String emailOrPhone, String securityQuestion, String securityAnswer,
            String newPassword) {
        this.emailOrPhone = emailOrPhone;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
        this.newPassword = newPassword;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
