package com.revpay.dto;

import com.revpay.enums.InvoiceStatus;
import com.revpay.model.Invoice;
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
public class InvoiceListResponse {

    private Long id;
    private String invoiceNumber;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
}