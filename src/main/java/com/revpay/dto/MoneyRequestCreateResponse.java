package com.revpay.dto;

import com.revpay.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoneyRequestCreateResponse {

    private Long requestId;
    private BigDecimal amount;
    private String purpose;
    private RequestStatus status;
    private LocalDateTime expiresAt;
}