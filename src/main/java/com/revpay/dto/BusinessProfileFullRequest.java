package com.revpay.dto;

import com.revpay.enums.BankAccountType;
import com.revpay.enums.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessProfileFullRequest {

    @NotBlank(message = "Username is Required.")
    private String username;

    @NotBlank(message = "Business name is mandatory")
    private String businessName;

    @NotNull
    private BusinessType businessType;

    @NotBlank(message = "Tax ID is mandatory")
    private String taxId;

    @NotBlank(message = "Address is mandatory")
    private String address;

    private String contactPhone;

    private String website;

    private String invoiceDetails;

    @NotBlank(message = "Account holder name is mandatory")
    private String accountHolderName;

    @NotBlank(message = "Bank name is mandatory")
    private String bankName;

    @NotBlank(message = "Account number is mandatory")
    private String accountNumber;

    @NotBlank(message = "IFSC code is mandatory")
    private String ifscCode;

    @NotNull
    private BankAccountType accountType;

    @NotNull(message = "Primary status is mandatory")
    private Boolean isPrimary = true;

}
