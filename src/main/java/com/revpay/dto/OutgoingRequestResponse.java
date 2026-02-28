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
public class OutgoingRequestResponse {

    private Long requestId;
    private ToInfo to;
    private BigDecimal amount;
    private String purpose;
    private RequestStatus status;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToInfo {
        private String name;
        private String email;
    }
}