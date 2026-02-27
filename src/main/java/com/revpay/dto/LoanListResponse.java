package com.revpay.dto;

import com.revpay.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanListResponse {

    private Long loanId;
    private BigDecimal loanAmount;
    private String purpose;
    private LoanStatus status;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private LocalDateTime submittedAt;
}