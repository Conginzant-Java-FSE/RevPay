package com.revpay.dto;

import com.revpay.enums.PaymentTerms;
import com.revpay.model.Invoice;
import jakarta.validation.Valid;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateInvoiceRequest {

    @Valid
    private CustomerInfo customer;

    @Valid
    private List<LineItemRequest> lineItems;

    private PaymentTerms paymentTerms;

    private LocalDate dueDate;

    private String notes;

    @Data
    public static class CustomerInfo {
        private String name;
        private String email;
        private String address;
    }

    @Data
    public static class LineItemRequest {
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
    }
}