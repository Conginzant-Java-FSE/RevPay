package com.revpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload for forgot password operation")
public class ForgotPasswordResponse {

    @Schema(description = "Result message", example = "Password reset successfully")
    private String message;

    @Schema(description = "Whether the operation was successful", example = "true")
    private boolean success;

    public ForgotPasswordResponse() {
    }

    public ForgotPasswordResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
