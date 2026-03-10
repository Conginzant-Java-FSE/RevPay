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
public class IncomingRequestResponse {

    private Long requestId;
    private FromInfo from;
    private BigDecimal amount;
    private String purpose;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FromInfo {
        private String name;
        private String email;
    }
}