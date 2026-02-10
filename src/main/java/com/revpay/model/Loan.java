package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.LoanStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loans")
public class Loan extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal loanAmount;

    private BigDecimal interestRate;

    private Integer tenureMonths;

    private BigDecimal emiAmount;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public Loan(Long loanId, User user, BigDecimal loanAmount, BigDecimal interestRate, Integer tenureMonths, BigDecimal emiAmount, LoanStatus status) {
        this.loanId = loanId;
        this.user = user;
        this.loanAmount = loanAmount;
        this.interestRate = interestRate;
        this.tenureMonths = tenureMonths;
        this.emiAmount = emiAmount;
        this.status = status;
    }

    public Loan() {}
}
