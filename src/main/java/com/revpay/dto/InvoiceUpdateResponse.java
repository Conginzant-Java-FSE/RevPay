package com.revpay.dto;

import com.revpay.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceUpdateResponse {

    private Long id;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
}