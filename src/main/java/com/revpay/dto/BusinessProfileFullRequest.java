package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BusinessProfileFullRequest {

    @NotBlank(message = "Business name is mandatory")
    private String businessName;

    @NotBlank(message = "Tax ID is mandatory")
    private String taxId;

    @NotBlank(message = "Address is mandatory")
    private String address;

    private String invoiceDetails;

    @NotBlank(message = "Account holder name is mandatory")
    private String accountHolderName;

    @NotBlank(message = "Bank name is mandatory")
    private String bankName;

    @NotBlank(message = "Account number is mandatory")
    private String accountNumber;

    @NotBlank(message = "IFSC code is mandatory")
    private String ifscCode;

    @NotNull(message = "Primary status is mandatory")
    private Boolean isPrimary;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInvoiceDetails() {
        return invoiceDetails;
    }

    public void setInvoiceDetails(String invoiceDetails) {
        this.invoiceDetails = invoiceDetails;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
