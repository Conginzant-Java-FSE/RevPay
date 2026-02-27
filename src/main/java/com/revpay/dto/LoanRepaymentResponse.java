package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanRepaymentResponse {

    private BigDecimal newBalance;
    private BigDecimal amountRepaid;
    private BigDecimal outstandingBalance;
}