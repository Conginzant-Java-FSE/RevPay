package com.revpay.service;

import com.revpay.dto.AcceptMoneyRequestRequest;
import com.revpay.dto.AcceptMoneyRequestResponse;
import com.revpay.dto.MoneyRequestCreateRequest;
import com.revpay.dto.MoneyRequestCreateResponse;
import com.revpay.enums.RequestStatus;
import com.revpay.model.MoneyRequest;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.model.Wallet;
import com.revpay.repository.MoneyRequestRepository;
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
@DisplayName("MoneyRequestService Unit Tests")
class MoneyRequestServiceTest {

    @Mock private MoneyRequestRepository moneyRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private MoneyRequestService moneyRequestService;

    private User requester;
    private User requestee;
    private Wallet requesterWallet;
    private Wallet requesteeWallet;
    private MoneyRequest pendingRequest;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setEmail("requester@example.com");
        requester.setFullName("Requester User");
        requester.setActive(true);

        requestee = new User();
        requestee.setId(2L);
        requestee.setEmail("requestee@example.com");
        requestee.setFullName("Requestee User");
        requestee.setMtPin("$2a$10$encodedPin");
        requestee.setActive(true);

        requesterWallet = new Wallet();
        requesterWallet.setWalletId(1L);
        requesterWallet.setUser(requester);
        requesterWallet.setBalance(new BigDecimal("500.00"));

        requesteeWallet = new Wallet();
        requesteeWallet.setWalletId(2L);
        requesteeWallet.setUser(requestee);
        requesteeWallet.setBalance(new BigDecimal("1000.00"));

        pendingRequest = new MoneyRequest();
        pendingRequest.setRequestId(10L);
        pendingRequest.setRequester(requester);
        pendingRequest.setRequestee(requestee);
        pendingRequest.setAmount(new BigDecimal("200.00"));
        pendingRequest.setPurpose("Lunch split");
        pendingRequest.setStatus(RequestStatus.PENDING);
        pendingRequest.setExpiresAt(LocalDateTime.now().plusDays(5));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("requestee@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    // CREATE REQUEST

    @Test
    @DisplayName("createRequest: should create money request and notify requestee")
    void createRequest_valid_shouldCreateAndNotify() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("requester@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        MoneyRequestCreateRequest dto = new MoneyRequestCreateRequest();
        dto.setRecipient("requestee@example.com");
        dto.setAmount(new BigDecimal("200.00"));
        dto.setPurpose("Lunch split");

        when(userRepository.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(moneyRequestRepository.save(any(MoneyRequest.class))).thenAnswer(inv -> {
            MoneyRequest mr = inv.getArgument(0);
            mr.setRequestId(10L);
            return mr;
        });

        MoneyRequestCreateResponse response = moneyRequestService.createRequest(dto);

        assertThat(response.getRequestId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(notificationService).sendNotification(eq(requestee), any(), anyString());
    }

    @Test
    @DisplayName("createRequest: should throw when requester requests from themselves")
    void createRequest_selfRequest_shouldThrow() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("requester@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        MoneyRequestCreateRequest dto = new MoneyRequestCreateRequest();
        dto.setRecipient("requester@example.com");
        dto.setAmount(new BigDecimal("100.00"));
        dto.setPurpose("Test");

        when(userRepository.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));

        assertThatThrownBy(() -> moneyRequestService.createRequest(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot request money from yourself");
    }

    // ACCEPT REQUEST

    @Test
    @DisplayName("acceptRequest: should transfer funds and update statuses")
    void acceptRequest_validPendingRequest_shouldTransferFunds() {
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(passwordEncoder.matches("1234", requestee.getMtPin())).thenReturn(true);
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));
        when(walletRepository.findByUser(requestee)).thenReturn(Optional.of(requesteeWallet));
        when(walletRepository.findByUser(requester)).thenReturn(Optional.of(requesterWallet));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setTransactionId(200L);
            return t;
        });

        AcceptMoneyRequestResponse response = moneyRequestService.acceptRequest(10L, request);

        assertThat(requesteeWallet.getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(requesterWallet.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(notificationService, times(2)).sendNotification(any(), any(), anyString());
    }

    @Test
    @DisplayName("acceptRequest: should throw when PIN is not set")
    void acceptRequest_pinNotSet_shouldThrow() {
        requestee.setMtPin(null);
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));

        assertThatThrownBy(() -> moneyRequestService.acceptRequest(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction PIN not set");
    }

    @Test
    @DisplayName("acceptRequest: should throw when PIN is incorrect")
    void acceptRequest_incorrectPin_shouldThrow() {
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("0000");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(passwordEncoder.matches("0000", requestee.getMtPin())).thenReturn(false);

        assertThatThrownBy(() -> moneyRequestService.acceptRequest(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Incorrect transaction PIN");
    }

    @Test
    @DisplayName("acceptRequest: should throw when request is already accepted")
    void acceptRequest_alreadyAccepted_shouldThrow() {
        pendingRequest.setStatus(RequestStatus.ACCEPTED);
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(passwordEncoder.matches("1234", requestee.getMtPin())).thenReturn(true);
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> moneyRequestService.acceptRequest(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending requests can be accepted");
    }

    @Test
    @DisplayName("acceptRequest: should throw when request has expired")
    void acceptRequest_expiredRequest_shouldThrow() {
        pendingRequest.setExpiresAt(LocalDateTime.now().minusDays(1));
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(passwordEncoder.matches("1234", requestee.getMtPin())).thenReturn(true);
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));
        when(walletRepository.findByUser(requestee)).thenReturn(Optional.of(requesteeWallet));

        assertThatThrownBy(() -> moneyRequestService.acceptRequest(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.EXPIRED);
    }

    @Test
    @DisplayName("acceptRequest: should throw when requestee has insufficient balance")
    void acceptRequest_insufficientBalance_shouldThrow() {
        requesteeWallet.setBalance(new BigDecimal("50.00")); // Less than 200
        AcceptMoneyRequestRequest request = new AcceptMoneyRequestRequest();
        request.setPin("1234");

        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(passwordEncoder.matches("1234", requestee.getMtPin())).thenReturn(true);
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));
        when(walletRepository.findByUser(requestee)).thenReturn(Optional.of(requesteeWallet));

        assertThatThrownBy(() -> moneyRequestService.acceptRequest(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient wallet balance");
    }

    // DECLINE REQUEST

    @Test
    @DisplayName("declineRequest: should update status to DECLINED")
    void declineRequest_pendingRequest_shouldDecline() {
        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));

        moneyRequestService.declineRequest(10L);

        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.DECLINED);
        verify(notificationService).sendNotification(eq(requester), any(), anyString());
    }

    @Test
    @DisplayName("declineRequest: should throw when request is not pending")
    void declineRequest_notPending_shouldThrow() {
        pendingRequest.setStatus(RequestStatus.DECLINED);
        when(userRepository.findByEmail("requestee@example.com")).thenReturn(Optional.of(requestee));
        when(moneyRequestRepository.findByRequestIdAndRequestee(10L, requestee)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> moneyRequestService.declineRequest(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending requests can be declined");
    }

    // CANCEL REQUEST

    @Test
    @DisplayName("cancelRequest: should update status to CANCELLED")
    void cancelRequest_pendingRequest_shouldCancel() {
        when(userRepository.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(moneyRequestRepository.findByRequestIdAndRequester(10L, requester)).thenReturn(Optional.of(pendingRequest));

        moneyRequestService.cancelRequest(10L, "requester@example.com");

        assertThat(pendingRequest.getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelRequest: should throw when request is already completed")
    void cancelRequest_alreadyCompleted_shouldThrow() {
        pendingRequest.setStatus(RequestStatus.ACCEPTED);
        when(userRepository.findByEmail("requester@example.com")).thenReturn(Optional.of(requester));
        when(moneyRequestRepository.findByRequestIdAndRequester(10L, requester)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> moneyRequestService.cancelRequest(10L, "requester@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only pending requests can be cancelled");
    }
}
