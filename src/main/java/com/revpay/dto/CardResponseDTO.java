package com.revpay.dto;

import com.revpay.enums.CardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CardResponseDTO {

    private Long cardId;
    private String lastFour;
    private CardType cardType;
    private Boolean isDefault;
}