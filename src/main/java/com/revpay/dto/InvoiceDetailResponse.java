package com.revpay.dto;

import com.revpay.enums.InvoiceStatus;
import com.revpay.enums.PaymentTerms;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceDetailResponse {

    private Long id;
    private String invoiceNumber;
    private CustomerInfo customer;
    private List<LineItemDetail> lineItems;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private PaymentTerms paymentTerms;
    private LocalDate dueDate;
    private String notes;
    private LocalDateTime sentAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerInfo {
        private String name;
        private String email;
        private String address;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LineItemDetail {
        private Long lineItemId;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;
        private BigDecimal lineTotal;
    }
}