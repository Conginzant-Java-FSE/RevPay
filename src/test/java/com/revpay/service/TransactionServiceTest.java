package com.revpay.service;

import com.revpay.dto.SendMoneyRequest;
import com.revpay.dto.TransactionResponseDTO;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.model.Wallet;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private NotificationService notificationService;
    @Mock private FraudDetectionService fraudDetectionService;
    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User receiver;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setFullName("Sender User");
        sender.setMtPin("$2a$10$encodedPin");
        sender.setActive(true);

        receiver = new User();
        receiver.setId(2L);
        receiver.setEmail("receiver@example.com");
        receiver.setFullName("Receiver User");
        receiver.setActive(true);

        senderWallet = new Wallet();
        senderWallet.setWalletId(1L);
        senderWallet.setUser(sender);
        senderWallet.setBalance(new BigDecimal("2000.00"));

        receiverWallet = new Wallet();
        receiverWallet.setWalletId(2L);
        receiverWallet.setUser(receiver);
        receiverWallet.setBalance(new BigDecimal("500.00"));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("sender@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(fraudDetectionService.checkFraud(anyLong(), anyDouble()))
                .thenReturn(false);
    }

    //  SEND MONEY

    @Test
    @DisplayName("sendMoney: should transfer funds between wallets successfully")
    void sendMoney_validRequest_shouldTransferFunds() {
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("500.00"));
        request.setPin("1234");
        request.setNote("Dinner payment");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUser(receiver)).thenReturn(Optional.of(receiverWallet));

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(100L);
        savedTransaction.setSender(sender);
        savedTransaction.setReceiver(receiver);
        savedTransaction.setAmount(new BigDecimal("500.00"));
        savedTransaction.setTransactionType(TransactionType.SEND);
        savedTransaction.setStatus(TransactionStatus.SUCCESS);
        savedTransaction.setBalanceAfter(new BigDecimal("1500.00"));
        savedTransaction.setCurrency("INR");
        savedTransaction.setNote("Dinner payment");
        savedTransaction.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.save(any())).thenReturn(savedTransaction);

        TransactionResponseDTO result = transactionService.sendMoney(request);

        assertThat(senderWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(receiverWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(notificationService, times(2)).sendNotification(any(), any(), anyString());
        assertThat(result.getType()).isEqualTo("SEND");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("sendMoney: should trigger low balance notification when balance drops below 100")
    void sendMoney_balanceDropsBelowThreshold_shouldSendLowBalanceAlert() {
        senderWallet.setBalance(new BigDecimal("150.00"));

        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUser(receiver)).thenReturn(Optional.of(receiverWallet));

        Transaction savedTransaction = new Transaction();
        savedTransaction.setTransactionId(101L);
        savedTransaction.setSender(sender);
        savedTransaction.setReceiver(receiver);
        savedTransaction.setAmount(new BigDecimal("100.00"));
        savedTransaction.setTransactionType(TransactionType.SEND);
        savedTransaction.setStatus(TransactionStatus.SUCCESS);
        savedTransaction.setBalanceAfter(new BigDecimal("50.00"));
        savedTransaction.setCurrency("INR");
        savedTransaction.setCreatedAt(LocalDateTime.now());
        when(transactionRepository.save(any())).thenReturn(savedTransaction);

        transactionService.sendMoney(request);

        // 2 regular + 1 low balance = 3 notifications
        verify(notificationService, times(3)).sendNotification(any(), any(), anyString());
    }

    @Test
    @DisplayName("sendMoney: should throw when sender PIN is not set")
    void sendMoney_pinNotSet_shouldThrow() {
        sender.setMtPin(null);
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction PIN not set");
    }

    @Test
    @DisplayName("sendMoney: should throw when PIN is incorrect")
    void sendMoney_incorrectPin_shouldThrow() {
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("9999");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("9999", sender.getMtPin())).thenReturn(false);

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect transaction PIN");
    }

    @Test
    @DisplayName("sendMoney: should throw when receiver is not found")
    void sendMoney_receiverNotFound_shouldThrow() {
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("unknown@example.com");
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Receiver not found");
    }

    @Test
    @DisplayName("sendMoney: should throw when sender tries to send money to themselves")
    void sendMoney_selfTransfer_shouldThrow() {
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("sender@example.com"); // same as sender
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com"))
                .thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot send money to yourself");
    }

    @Test
    @DisplayName("sendMoney: should throw when receiver account is inactive")
    void sendMoney_inactiveReceiver_shouldThrow() {
        receiver.setActive(false);
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Receiver account is inactive");
    }

    @Test
    @DisplayName("sendMoney: should throw when sender wallet balance is insufficient")
    void sendMoney_insufficientBalance_shouldThrow() {
        SendMoneyRequest request = new SendMoneyRequest();
        request.setReceiverEmailOrPhone("receiver@example.com");
        request.setAmount(new BigDecimal("9999.00")); // More than 2000 balance
        request.setPin("1234");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(passwordEncoder.matches("1234", sender.getMtPin())).thenReturn(true);
        when(userRepository.findByEmail("receiver@example.com")).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));

        assertThatThrownBy(() -> transactionService.sendMoney(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient wallet balance");
    }

    //  GET TRANSACTION BY ID

    @Test
    @DisplayName("getTransactionById: should throw when transaction does not belong to user")
    void getTransactionById_notBelonging_shouldThrow() {
        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(transactionRepository.findByIdAndUser(999L, sender)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction not found with id: 999");
    }
}
