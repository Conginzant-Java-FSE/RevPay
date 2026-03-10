package com.revpay.dto;

import com.revpay.enums.PaymentTerms;
import com.revpay.model.Invoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {

    @NotNull(message = "Customer information is required")
    @Valid
    private CustomerInfo customer;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<LineItemRequest> lineItems;

    private PaymentTerms paymentTerms;

    private LocalDate dueDate;

    private String notes;

    @Data
    public static class CustomerInfo {

        @NotBlank(message = "Customer name is required")
        private String name;

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid customer email")
        private String email;

        private String address;
    }

    @Data
    public static class LineItemRequest {

        @NotBlank(message = "Line item description is required")
        private String description;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        private BigDecimal unitPrice;

        private BigDecimal taxRate;
    }
}