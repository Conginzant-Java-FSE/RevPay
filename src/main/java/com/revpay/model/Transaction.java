package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
public class Transaction extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String note;

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Transaction(Long id, User sender, User receiver, BigDecimal amount, TransactionType transactionType, TransactionStatus status, String note) {
        this.transactionId = id;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.transactionType = transactionType;
        this.status = status;
        this.note = note;
    }

    public Transaction() {}
}
