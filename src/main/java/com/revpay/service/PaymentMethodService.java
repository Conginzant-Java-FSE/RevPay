package com.revpay.service;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.CardResponseDTO;
import com.revpay.dto.PaymentMethodListDTO;
import com.revpay.dto.UpdateCardRequest;
import com.revpay.enums.CardType;
import com.revpay.enums.RecordStatus;
import com.revpay.model.PaymentMethod;
import com.revpay.model.User;
import com.revpay.repository.PaymentMethodRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentMethodService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMethodService.class);

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
                paymentMethod.getIsDefault());
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

    public List<PaymentMethodListDTO> getUserCards(Long userId) {
        List<PaymentMethod> cards = paymentMethodRepository.findByUser_IdAndStatus(userId, RecordStatus.ACTIVE);

        return cards.stream().map(card -> {

            String billingAddress = String.join(", ",
                    Optional.ofNullable(card.getBillingStreet()).orElse(""),
                    Optional.ofNullable(card.getBillingCity()).orElse(""),
                    Optional.ofNullable(card.getBillingState()).orElse(""),
                    Optional.ofNullable(card.getBillingZip()).orElse(""),
                    Optional.ofNullable(card.getBillingCountry()).orElse(""));

            return PaymentMethodListDTO.builder()
                    .cardId(card.getCardId())
                    .nickname(card.getNickname())
                    .cardType(card.getCardType())
                    .lastFour(card.getLastFour())
                    .expiryMonth(card.getExpiryMonth())
                    .expiryYear(card.getExpiryYear())
                    .isDefault(card.getIsDefault())
                    .billingAddress(billingAddress.trim())
                    .build();

        }).toList();
    }

    @Transactional
    public void updateCard(Long cardId, UpdateCardRequest request) {

        User user = getLoggedInUser();

        PaymentMethod card = paymentMethodRepository.findByCardIdAndUser(cardId, user)
                .orElseThrow(() -> new IllegalArgumentException("Card not found or does not belong to this account"));

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            card.setNickname(request.getNickname());
        }

        if (request.getBillingStreet() != null && !request.getBillingStreet().isBlank()) {
            card.setBillingStreet(request.getBillingStreet());
        }

        if (request.getBillingCity() != null && !request.getBillingCity().isBlank()) {
            card.setBillingCity(request.getBillingCity());
        }

        if (request.getBillingState() != null && !request.getBillingState().isBlank()) {
            card.setBillingState(request.getBillingState());
        }

        if (request.getBillingZip() != null && !request.getBillingZip().isBlank()) {
            card.setBillingZip(request.getBillingZip());
        }

        if (request.getBillingCountry() != null && !request.getBillingCountry().isBlank()) {
            card.setBillingCountry(request.getBillingCountry());
        }

        paymentMethodRepository.save(card);

        logger.info("Card {} updated for user: {}", cardId, user.getEmail());
    }

    @Transactional
    public void deleteCard(Long cardId) {

        User user = getLoggedInUser();

        PaymentMethod card = paymentMethodRepository.findByCardIdAndUser(cardId, user)
                .orElseThrow(() -> new IllegalArgumentException("Card not found or does not belong to this account"));

        boolean wasDefault = card.getIsDefault();

        card.setStatus(RecordStatus.DELETED);
        card.setIsDefault(false);
        paymentMethodRepository.save(card);

        if (wasDefault) {
            List<PaymentMethod> remainingCards = paymentMethodRepository.findByUser_IdAndStatus(user.getId(),
                    RecordStatus.ACTIVE);
            if (!remainingCards.isEmpty()) {
                PaymentMethod newDefault = remainingCards.get(0);
                newDefault.setIsDefault(true);
                paymentMethodRepository.save(newDefault);
                logger.info("Promoted card {} to default for user: {}", newDefault.getCardId(), user.getEmail());
            } else {
                logger.info("No remaining active cards for user {}. Default remains null.", user.getEmail());
            }
        }

        logger.info("Card {} deleted for user: {}", cardId, user.getEmail());
    }
}
