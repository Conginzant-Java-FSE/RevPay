package com.revpay.dto;

import lombok.Data;

@Data
public class UpdateCardRequest {

    private String nickname;
    private String billingStreet;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
}