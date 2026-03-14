package com.revpay.model;

import com.revpay.config.AuditConfig;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "split_payments")
public class SplitPayment extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 255)
    private String note;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, SETTLED

    public SplitPayment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
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
}