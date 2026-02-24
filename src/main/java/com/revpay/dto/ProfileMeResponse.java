package com.revpay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.revpay.enums.AccountType;
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
@JsonInclude(JsonInclude.Include.NON_NULL) // hides null fields from response
public class ProfileMeResponse {

    private Long userId;
    private AccountType accountType;
    private String fullName;
    private String email;
    private String phone;
    private Boolean profileComplete;
    private BigDecimal walletBalance;
    private LocalDateTime createdAt;

    // Personal profile fields
    private String address;
    private String dob;

    // Business profile fields
    private String businessName;
    private String businessType;
    private String taxId;
    private String contactPhone;
    private String website;
    private String businessStatus;

    // Nested bank account
    private BankAccountSummary bankAccount;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BankAccountSummary {
        private String bankName;
        private String accountNumber; // masked
        private String accountType;
    }
}