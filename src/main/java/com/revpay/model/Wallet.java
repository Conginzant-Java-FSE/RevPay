package com.revpay.model;

import com.revpay.config.AuditConfig;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallet",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id")
        })
public class Wallet extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private BigDecimal balance;

    private String currency;

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Wallet(Long walletId, User user, BigDecimal balance, String currency) {
        this.walletId = walletId;
        this.user = user;
        this.balance = balance;
        this.currency = currency;
    }

    public Wallet() {}
}
