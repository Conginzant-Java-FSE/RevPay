package com.revpay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SplitPaymentResponse {

    private Long splitId;
    private String createdByName;
    private BigDecimal totalAmount;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private List<ParticipantResponse> participants;

    public SplitPaymentResponse() {}

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ParticipantResponse> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ParticipantResponse> participants) {
        this.participants = participants;
    }

    public static class ParticipantResponse {

        private String fullName;
        private String emailOrPhone;
        private BigDecimal amountOwed;
        private boolean paid;
        private LocalDateTime paidAt;

        public ParticipantResponse() {}

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmailOrPhone() {
            return emailOrPhone;
        }

        public void setEmailOrPhone(String emailOrPhone) {
            this.emailOrPhone = emailOrPhone;
        }

        public BigDecimal getAmountOwed() {
            return amountOwed;
        }

        public void setAmountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
        }

        public boolean isPaid() {
            return paid;
        }

        public void setPaid(boolean paid) {
            this.paid = paid;
        }

        public LocalDateTime getPaidAt() {
            return paidAt;
        }

        public void setPaidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
        }
    }
}