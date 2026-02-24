package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WalletBalanceResponse {

    private BigDecimal balance;
    private String currency;
    private LocalDateTime lastUpdated;
}