package com.revpay.model;

import com.revpay.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "loan_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal loanAmount;

    @Column(name = "emi_amount", precision = 38, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "interest_rate", precision = 38, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "total_interest", precision = 38, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_repayable", precision = 38, scale = 2)
    private BigDecimal totalRepayable;

    @Column(name = "amount_repaid", precision = 38, scale = 2)
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", precision = 38, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "annual_revenue", precision = 38, scale = 2)
    private BigDecimal annualRevenue;

    @Column(name = "years_in_business")
    private Integer yearsInBusiness;

    @Column(name = "employee_count")
    private Integer employeeCount;

    private String collateral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
