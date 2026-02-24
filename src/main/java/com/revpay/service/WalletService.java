package com.revpay.service;

import com.revpay.dto.AddFundsRequest;
import com.revpay.enums.NotificationType;
import com.revpay.enums.RecordStatus;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.*;
import com.revpay.repository.PaymentMethodRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void addFunds(AddFundsRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        //Verify transaction PIN
        if (user.getMtPin() == null) {
            throw new IllegalStateException("Transaction PIN not set. Please set your PIN before making transactions.");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getMtPin())) {
            throw new IllegalArgumentException("Incorrect transaction PIN");
        }

        //Verify card belongs to this user
        PaymentMethod card = paymentMethodRepository
                .findByCardIdAndUser(Long.parseLong(request.getCardId()), user)
                .orElseThrow(() -> new IllegalArgumentException("Card not found or does not belong to this account"));

        if (card.getStatus() != RecordStatus.ACTIVE) {
            throw new IllegalStateException("Card is inactive or expired");
        }

        //Credit the wallet
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for this user"));

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.add(request.getAmount());

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        //Create AddMoney transaction record
        Transaction transaction = new Transaction();
        transaction.setSender(null);
        transaction.setReceiver(user);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.ADD_MONEY);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setNote("Wallet top-up from card ending " + card.getLastFour());
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);

        //Send notification
        notificationService.sendNotification(
                user, NotificationType.TRANSACTION_RECEIVED,
                "₹" + request.getAmount() + " added to your wallet from card ending " + card.getLastFour()
        );

        logger.info("Wallet top-up of {} completed for user: {}", request.getAmount(), user.getEmail());
    }

}
