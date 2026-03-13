package com.revpay.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTransactionDTO {

    private Long transactionId;


    private BigDecimal amount;

    private String type;

    private String time;

    private String date;

    private String status;

    private String note;

    private String counterpartyName;

    private String utrNumber;
}