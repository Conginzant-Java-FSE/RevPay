package com.revpay.dto;

import java.math.BigDecimal;
import java.util.List;

public class CreateSplitRequest {
    private BigDecimal totalAmount;
    private String note;
    private String pin;
    private List<SplitParticipantDTO> participants;

    public CreateSplitRequest() {}

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public List<SplitParticipantDTO> getParticipants() { return participants; }
    public void setParticipants(List<SplitParticipantDTO> participants) { this.participants = participants; }

    public static class SplitParticipantDTO {
        private String emailOrPhone;
        private BigDecimal amount;

        public SplitParticipantDTO() {}

        public String getEmailOrPhone() { return emailOrPhone; }
        public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
