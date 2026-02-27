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
public class LoanApplicationResponse {

    private Long loanId;
    private LoanStatus status;
    private BigDecimal loanAmount;
    private LocalDateTime submittedAt;
}