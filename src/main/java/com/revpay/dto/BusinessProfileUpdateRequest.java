package com.revpay.dto;

import com.revpay.enums.BusinessType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessProfileUpdateRequest {

    private String businessName;
    private BusinessType businessType;
    private String taxId;
    private String contactPhone;
    private String website;
}