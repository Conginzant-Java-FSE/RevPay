package com.revpay.service;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.CardResponseDTO;
import com.revpay.enums.CardType;
import com.revpay.enums.RecordStatus;
import com.revpay.model.PaymentMethod;
import com.revpay.model.User;
import com.revpay.repository.PaymentMethodRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

@Service
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CardResponseDTO addCard(AddCardRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ===== Validation =====
        validateCardDetails(request);

        // ===== Detect Card Type =====
        CardType cardType = detectCardType(request.getCardNumber());

        // ===== Tokenization =====
        String token = UUID.randomUUID().toString();
        String lastFour = request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);

        // ===== Handle Default Logic =====
        boolean isDefault = Boolean.TRUE.equals(request.getSetAsDefault());

        if (isDefault) {
            paymentMethodRepository.unsetAllDefaultsByUser(user);
        }

        // ===== Save Payment Method =====
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .cardToken(token)
                .cardHolderName(request.getCardHolderName())
                .cardType(cardType)
                .lastFour(lastFour)
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .nickname(request.getNickname())
                .isDefault(isDefault)
                .billingStreet(request.getBillingStreet())
                .billingCity(request.getBillingCity())
                .billingState(request.getBillingState())
                .billingZip(request.getBillingZip())
                .billingCountry(request.getBillingCountry())
                .status(RecordStatus.ACTIVE)
                .user(user)
                .build();

        paymentMethodRepository.save(paymentMethod);

        return new CardResponseDTO(
                paymentMethod.getCardId(),
                paymentMethod.getLastFour(),
                paymentMethod.getCardType(),
                paymentMethod.getIsDefault()
        );
    }

    private void validateCardDetails(AddCardRequest request) {

        if (request.getCardNumber() == null || request.getCardNumber().length() != 16) {
            throw new IllegalArgumentException("Card number must be 16 digits");
        }

        if (request.getCvv() == null ||
                !(request.getCvv().length() == 3 || request.getCvv().length() == 4)) {
            throw new IllegalArgumentException("Invalid CVV");
        }

        if (request.getExpiryYear() < Year.now().getValue()) {
            throw new IllegalArgumentException("Card expired");
        }
    }

    private CardType detectCardType(String cardNumber) {

        if (cardNumber.startsWith("4")) {
            return CardType.VISA;
        } else if (cardNumber.startsWith("5")) {
            return CardType.MASTERCARD;
        } else if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) {
            return CardType.AMEX;
        } else if (cardNumber.startsWith("6")) {
            return CardType.DISCOVER;
        } else {
            return CardType.UNKNOWN;
        }
    }
}
