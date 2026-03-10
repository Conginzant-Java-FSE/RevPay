package com.revpay.service;

import com.revpay.dto.AddFundsRequest;
import com.revpay.dto.WalletBalanceResponse;
import com.revpay.dto.WithdrawRequest;
import com.revpay.model.BankAccount;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.model.Wallet;
import com.revpay.repository.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private NotificationService notificationService;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private WalletService walletService;

    private User mockUser;
    private Wallet mockWallet;
    private BankAccount mockBankAccount;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setMtPin("$2a$10$encodedPin");
        mockUser.setActive(true);

        mockWallet = new Wallet();
        mockWallet.setWalletId(1L);
        mockWallet.setUser(mockUser);
        mockWallet.setBalance(new BigDecimal("1000.00"));
        mockWallet.setCurrency("INR");

        mockBankAccount = new BankAccount();
        mockBankAccount.setAccountId(1L);
        mockBankAccount.setUser(mockUser);
        mockBankAccount.setBankName("HDFC Bank");
        mockBankAccount.setAccountNumber("123456789012");
        mockBankAccount.setIsPrimary(true);

        // Set up security context
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    //ADD FUNDS

    @Test
    @DisplayName("addFunds: should add funds to wallet and create transaction record")
    void addFunds_validRequest_shouldTopUpWallet() {
        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("1234", mockUser.getMtPin())).thenReturn(true);
        when(bankAccountRepository.findByUserAndIsPrimaryTrue(mockUser)).thenReturn(Optional.of(mockBankAccount));
        when(walletRepository.findByUser(mockUser)).thenReturn(Optional.of(mockWallet));

        walletService.addFunds(request);

        assertThat(mockWallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
        verify(walletRepository).save(mockWallet);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationService).sendNotification(any(), any(), anyString());
    }

    @Test
    @DisplayName("addFunds: should throw when transaction PIN not set")
    void addFunds_pinNotSet_shouldThrow() {
        mockUser.setMtPin(null);
        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> walletService.addFunds(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction PIN not set");
    }

    @Test
    @DisplayName("addFunds: should throw when PIN is incorrect")
    void addFunds_incorrectPin_shouldThrow() {
        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPin("0000");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("0000", mockUser.getMtPin())).thenReturn(false);

        assertThatThrownBy(() -> walletService.addFunds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect transaction PIN");
    }

    @Test
    @DisplayName("addFunds: should throw when no bank account linked")
    void addFunds_noBankAccount_shouldThrow() {
        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("1234", mockUser.getMtPin())).thenReturn(true);
        when(bankAccountRepository.findByUserAndIsPrimaryTrue(mockUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.addFunds(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No bank account linked");
    }

    //WITHDRAW FUNDS

    @Test
    @DisplayName("withdrawFunds: should deduct funds and create transaction record")
    void withdrawFunds_sufficientBalance_shouldWithdraw() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("300.00"));
        request.setPin("1234");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("1234", mockUser.getMtPin())).thenReturn(true);
        when(bankAccountRepository.findByUserAndIsPrimaryTrue(mockUser)).thenReturn(Optional.of(mockBankAccount));
        when(walletRepository.findByUser(mockUser)).thenReturn(Optional.of(mockWallet));

        walletService.withdrawFunds(request);

        assertThat(mockWallet.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        verify(walletRepository).save(mockWallet);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("withdrawFunds: should throw when balance is insufficient")
    void withdrawFunds_insufficientBalance_shouldThrow() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("5000.00")); // More than balance of 1000
        request.setPin("1234");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("1234", mockUser.getMtPin())).thenReturn(true);
        when(bankAccountRepository.findByUserAndIsPrimaryTrue(mockUser)).thenReturn(Optional.of(mockBankAccount));
        when(walletRepository.findByUser(mockUser)).thenReturn(Optional.of(mockWallet));

        assertThatThrownBy(() -> walletService.withdrawFunds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient wallet balance");
    }

    @Test
    @DisplayName("withdrawFunds: should throw when PIN is incorrect")
    void withdrawFunds_incorrectPin_shouldThrow() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setPin("9999");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("9999", mockUser.getMtPin())).thenReturn(false);

        assertThatThrownBy(() -> walletService.withdrawFunds(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect transaction PIN");
    }

    //GET WALLET BALANCE

    @Test
    @DisplayName("getWalletBalance: should return current balance and currency")
    void getWalletBalance_shouldReturnBalanceResponse() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUser(mockUser)).thenReturn(Optional.of(mockWallet));

        WalletBalanceResponse response = walletService.getWalletBalance();

        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("getWalletBalance: should throw when wallet not found")
    void getWalletBalance_noWallet_shouldThrow() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(walletRepository.findByUser(mockUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletBalance())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Wallet not found");
    }
}
