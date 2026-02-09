package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
public class BankAccount extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String accountHolderName;

    private String bankName;

    private String accountNumber;

    private String ifscCode;

    private Boolean isPrimary;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public Boolean getPrimary() {
        return isPrimary;
    }

    public void setPrimary(Boolean primary) {
        isPrimary = primary;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public BankAccount(Long accountId, User user, String accountHolderName, String bankName, String accountNumber, String ifscCode, Boolean isPrimary, RecordStatus status) {
        this.accountId = accountId;
        this.user = user;
        this.accountHolderName = accountHolderName;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.isPrimary = isPrimary;
        this.status = status;
    }

    public BankAccount() {}
}
