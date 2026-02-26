package com.revpay.dto;

import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TransactionFilterRequest {


    private int page = 0;


    private int size = 10;

    /**
     * Filter by transaction type.
     * Accepted values: SEND, REQUEST, ADD_MONEY, WITHDRAW, LOAN_REPAYMENT
     */
    private TransactionType type;


    private TransactionStatus status;


    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;


    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;


    private String search;
}