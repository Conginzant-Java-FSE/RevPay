package com.revpay.service;

import com.revpay.dto.AddFundsRequest;
import com.revpay.dto.BankAccountResponse;
import com.revpay.enums.NotificationType;
import com.revpay.enums.RecordStatus;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.*;
import com.revpay.repository.*;
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
    private BankAccountRepository bankAccountRepository;

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

        // ── Verify transaction PIN
        if (user.getMtPin() == null) {
            throw new IllegalStateException("Transaction PIN not set. Please set your PIN first.");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getMtPin())) {
            throw new IllegalArgumentException("Incorrect transaction PIN");
        }

        // ── Get user's primary bank account
        BankAccount bankAccount = bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new IllegalStateException(
                        "No bank account linked. Please complete your profile first."));

        // ── Credit the wallet
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for this user"));

        BigDecimal balanceAfter = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        // ── Create TOPUP transaction record
        Transaction transaction = new Transaction();
        transaction.setSender(null);
        transaction.setReceiver(user);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.ADD_MONEY);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setNote("Added from " + bankAccount.getBankName()
                + " account ending "
                + getLast4(bankAccount.getAccountNumber()));
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // ── Send notification
        notificationService.sendNotification(
                user,
                NotificationType.TRANSACTION_RECEIVED,
                "₹" + request.getAmount() + " added to your wallet from "
                        + bankAccount.getBankName()
                        + " account ending " + getLast4(bankAccount.getAccountNumber())
        );

        logger.info("Wallet top-up of {} completed for user: {}", request.getAmount(), user.getEmail());
    }

    private String getLast4(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return accountNumber.substring(accountNumber.length() - 4);
    }

    public BankAccountResponse getLinkedBankAccount() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        BankAccount bankAccount = bankAccountRepository
                .findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() ->
                        new IllegalStateException("No primary bank account linked"));

        String maskedAccount = maskAccountNumber(bankAccount.getAccountNumber());

        return new BankAccountResponse(
                bankAccount.getBankName(),
                maskedAccount,
                bankAccount.getAccountType()
        );
    }
    private String maskAccountNumber(String accountNumber) {

        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }

        int visibleDigits = 4;
        int length = accountNumber.length();

        return "*".repeat(length - visibleDigits)
                + accountNumber.substring(length - visibleDigits);
    }
}
