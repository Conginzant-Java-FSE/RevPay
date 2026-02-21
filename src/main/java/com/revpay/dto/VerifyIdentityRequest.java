package com.revpay.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyIdentityRequest {
    private String emailOrPhone;
}