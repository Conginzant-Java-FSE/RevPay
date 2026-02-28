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
public class AcceptMoneyRequestResponse {

    private Long transactionId;
    private BigDecimal amount;
    private BigDecimal newBalance;
}