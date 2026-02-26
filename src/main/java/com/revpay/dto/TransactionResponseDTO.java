package com.revpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponseDTO {

    private Long transactionId;


    private String type;


    private String status;

    private BigDecimal amount;
    private String currency;
    private String note;


    private CounterpartyDTO counterparty;

    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;


    private LocalDateTime completedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CounterpartyDTO {
        private Long userId;
        private String fullName;
        private String email;
    }
}