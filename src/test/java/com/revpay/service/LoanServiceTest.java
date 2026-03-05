package com.revpay.service;

import com.revpay.dto.LoanApplicationRequest;
import com.revpay.dto.LoanApplicationResponse;
import com.revpay.enums.AccountType;
import com.revpay.enums.LoanStatus;
import com.revpay.model.Loan;
import com.revpay.model.User;
import com.revpay.model.Wallet;
import com.revpay.repository.LoanRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LoanService loanService;

    private User businessUser;
    private Wallet businessWallet;

    @BeforeEach
    void setUp() {
        businessUser = new User();
        businessUser.setId(1L);
        businessUser.setEmail("business@example.com");
        businessUser.setFullName("Business Owner");
        businessUser.setAccountType(AccountType.BUSINESS);
        businessUser.setActive(true);

        businessWallet = new Wallet();
        businessWallet.setWalletId(1L);
        businessWallet.setUser(businessUser);
        businessWallet.setBalance(new BigDecimal("5000.00"));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("business@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    //APPLY FOR LOAN

    @Test
    @DisplayName("applyForLoan: should auto-approve loans below 50000 threshold")
    void applyForLoan_belowThreshold_shouldAutoApprove() {
        LoanApplicationRequest request = buildRequest(new BigDecimal("30000.00"), 12);

        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(businessUser));
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.PENDING)).thenReturn(false);
        when(walletRepository.findByUser(businessUser)).thenReturn(Optional.of(businessWallet));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setLoanId(1L);
            return l;
        });

        LoanApplicationResponse response = loanService.applyForLoan(request);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.getLoanAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
        // Wallet should be credited
        assertThat(businessWallet.getBalance()).isGreaterThan(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("applyForLoan: should set status to PENDING for loans at or above 50000")
    void applyForLoan_aboveThreshold_shouldBePending() {
        LoanApplicationRequest request = buildRequest(new BigDecimal("75000.00"), 24);

        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(businessUser));
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.PENDING)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setLoanId(2L);
            return l;
        });

        LoanApplicationResponse response = loanService.applyForLoan(request);

        assertThat(response.getStatus()).isEqualTo(LoanStatus.PENDING);
    }

    @Test
    @DisplayName("applyForLoan: should throw when user is not a business account")
    void applyForLoan_personalAccount_shouldThrow() {
        businessUser.setAccountType(AccountType.PERSONAL);
        LoanApplicationRequest request = buildRequest(new BigDecimal("10000.00"), 6);

        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(businessUser));

        assertThatThrownBy(() -> loanService.applyForLoan(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Business account required");
    }

    @Test
    @DisplayName("applyForLoan: should throw when user already has an active loan")
    void applyForLoan_activeLoanExists_shouldThrow() {
        LoanApplicationRequest request = buildRequest(new BigDecimal("10000.00"), 6);

        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(businessUser));
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> loanService.applyForLoan(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active loan");
    }

    @Test
    @DisplayName("applyForLoan: should throw when a pending loan application already exists")
    void applyForLoan_pendingLoanExists_shouldThrow() {
        LoanApplicationRequest request = buildRequest(new BigDecimal("10000.00"), 6);

        when(userRepository.findByEmail("business@example.com")).thenReturn(Optional.of(businessUser));
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserAndStatus(businessUser, LoanStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> loanService.applyForLoan(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending loan application");
    }

    // HELPER

    private LoanApplicationRequest buildRequest(BigDecimal amount, int tenure) {
        LoanApplicationRequest req = new LoanApplicationRequest();
        req.setLoanAmount(amount);
        req.setPurpose("Business expansion");
        req.setTenureMonths(tenure);
        req.setAnnualRevenue(new BigDecimal("500000.00"));
        req.setYearsInBusiness(3);
        req.setEmployeeCount(10);
        req.setCollateral("Equipment");
        return req;
    }
}
