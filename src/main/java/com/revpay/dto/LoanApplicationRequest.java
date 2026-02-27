package com.revpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1,000")
    private BigDecimal loanAmount;

    @NotBlank(message = "Loan purpose is required")
    private String purpose;

    @NotNull(message = "Tenure is required")
    @Min(value = 3, message = "Minimum tenure is 3 months")
    private Integer tenureMonths;

    @NotNull(message = "Annual revenue is required")
    @DecimalMin(value = "0.00", message = "Annual revenue cannot be negative")
    private BigDecimal annualRevenue;

    @NotNull(message = "Years in business is required")
    @Min(value = 0, message = "Years in business cannot be negative")
    private Integer yearsInBusiness;

    private Integer employeeCount;

    private String collateral;
}