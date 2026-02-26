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
public class AnalyticsSummaryResponse {

    private BigDecimal totalReceived;
    private BigDecimal totalSent;
    private BigDecimal pendingIncoming;
    private BigDecimal pendingOutgoing;
    private BigDecimal netFlow;
}