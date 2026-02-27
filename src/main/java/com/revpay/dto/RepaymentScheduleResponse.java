package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentScheduleResponse {

    private Integer instalmentNumber;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principal;
    private BigDecimal interest;
    private InstalmentStatus status;

    public enum InstalmentStatus {
        PAID, UPCOMING, OVERDUE
    }
}