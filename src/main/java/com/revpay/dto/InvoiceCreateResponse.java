package com.revpay.dto;

import com.revpay.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceCreateResponse {

    private Long id;
    private String invoiceNumber;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
}