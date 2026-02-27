package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceSummaryResponse {

    private BigDecimal totalPaid;
    private BigDecimal totalUnpaid;
    private BigDecimal totalOverdue;
    private InvoiceCount invoiceCount;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InvoiceCount {
        private Long paid;
        private Long unpaid;
        private Long overdue;
    }
}