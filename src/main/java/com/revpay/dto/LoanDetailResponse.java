package com.revpay.dto;

import com.revpay.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanDetailResponse {

    private Long loanId;
    private LoanStatus status;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayable;
    private BigDecimal amountRepaid;
    private BigDecimal outstandingBalance;
    private LocalDate nextDueDate;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;  // only populated when status = REJECTED
}