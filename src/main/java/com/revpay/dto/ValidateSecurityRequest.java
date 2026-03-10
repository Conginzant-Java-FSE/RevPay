package com.revpay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateSecurityRequest {

    private String emailOrPhone;
    private String securityQuestion;
    private String securityAnswer;

}