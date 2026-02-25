package com.revpay.service;

import com.revpay.dto.AddFundsRequest;
import com.revpay.dto.UpdateBankAccountRequest;
import com.revpay.dto.WalletBalanceResponse;
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

        if (user.getMtPin() == null) {
            throw new IllegalStateException("Transaction PIN not set. Please set your PIN first.");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getMtPin())) {
            throw new IllegalArgumentException("Incorrect transaction PIN");
        }

        BankAccount bankAccount = bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new IllegalStateException(
                        "No bank account linked. Please complete your profile first."));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for this user"));

        BigDecimal balanceAfter = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

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

    public WalletBalanceResponse getWalletBalance() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        return WalletBalanceResponse.builder()
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .lastUpdated(wallet.getUpdatedAt())   // comes from AuditConfig
                .build();
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

    @Transactional
    public void updateBankAccount(UpdateBankAccountRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        BankAccount bankAccount = bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new IllegalStateException("No primary bank account found"));

        if (request.getBankName() != null && !request.getBankName().isBlank()) {
            bankAccount.setBankName(request.getBankName());
        }

        if (request.getAccountNumber() != null && !request.getAccountNumber().isBlank()) {
            bankAccount.setAccountNumber(maskAccountNumber(request.getAccountNumber()));
        }

        if (request.getIfscCode() != null && !request.getIfscCode().isBlank()) {
            bankAccount.setIfscCode(request.getIfscCode());
        }

        if (request.getAccountType() != null) {
            bankAccount.setAccountType(request.getAccountType());
        }

        bankAccountRepository.save(bankAccount);

        notificationService.sendNotification(
                user,
                NotificationType.GENERAL,
                "Your linked bank account has been updated successfully"
        );

        logger.info("Bank account updated for user: {}", user.getEmail());
    }

}
