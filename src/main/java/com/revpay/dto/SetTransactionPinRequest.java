package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SetTransactionPinRequest {

    @NotBlank(message = "MTPIN is required")
    @Pattern(regexp = "\\d{4}", message = "MTPIN must be exactly 4 digits")
    private String mtPin;

    @NotBlank(message = "Confirm MTPIN is required")
    private String confirmMtPin;

    public String getMtPin() {
        return mtPin;
    }

    public void setMtPin(String mtPin) {
        this.mtPin = mtPin;
    }

    public String getConfirmMtPin() {
        return confirmMtPin;
    }

    public void setConfirmMtPin(String confirmMtPin) {
        this.confirmMtPin = confirmMtPin;
    }
}
