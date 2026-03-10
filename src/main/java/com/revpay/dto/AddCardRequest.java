package com.revpay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCardRequest {

    private String cardNumber;      // 16 digits
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cvv;             // used only for validation
    private String cardHolderName;
    private String nickname;
    private Boolean setAsDefault;
    private String billingStreet;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
}