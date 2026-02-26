package com.revpay.dto;

import com.revpay.enums.CardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PaymentMethodListDTO {

    private Long cardId;
    private String nickname;
    private CardType cardType;
    private String lastFour;
    private Integer expiryMonth;
    private Integer expiryYear;
    private Boolean isDefault;
    private String billingAddress;
}
