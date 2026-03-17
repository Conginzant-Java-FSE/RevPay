package com.revpay.service;

import com.revpay.dto.CreateSplitRequest;
import com.revpay.dto.SplitPaymentResponse;
import com.revpay.enums.NotificationType;
import com.revpay.model.*;
import com.revpay.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SplitPaymentServiceTest {

    @Mock
    private SplitPaymentRepository splitPaymentRepository;
    @Mock
    private SplitPaymentParticipantRepository participantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private SplitPaymentService splitPaymentService;

    private User creator;
    private User participant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("creator@example.com");

        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@example.com");
        creator.setFullName("Creator User");
        creator.setMtPin("encoded-pin");

        participant = new User();
        participant.setId(2L);
        participant.setEmail("participant@example.com");
        participant.setFullName("Participant User");

        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));
        when(userRepository.findByEmail("participant@example.com")).thenReturn(Optional.of(participant));
    }

    @Test
    void testCreateSplit_Success() {
        CreateSplitRequest request = new CreateSplitRequest();
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setNote("Dinner");
        request.setPin("1234");
        
        CreateSplitRequest.SplitParticipantDTO pDto = new CreateSplitRequest.SplitParticipantDTO();
        pDto.setEmailOrPhone("participant@example.com");
        pDto.setAmount(new BigDecimal("50.00"));
        request.setParticipants(Collections.singletonList(pDto));

        when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
        when(splitPaymentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        SplitPaymentResponse response = splitPaymentService.createSplit(request);

        assertNotNull(response);
        assertEquals("Dinner", response.getNote());
        verify(splitPaymentRepository, times(1)).save(any(SplitPayment.class));
        verify(participantRepository, times(1)).save(any(SplitPaymentParticipant.class));
        verify(notificationService, times(1)).sendNotification(eq(participant), eq(NotificationType.GENERAL), anyString());
    }

    @Test
    void testCreateSplit_IncorrectPin() {
        CreateSplitRequest request = new CreateSplitRequest();
        request.setPin("wrong");

        when(passwordEncoder.matches("wrong", "encoded-pin")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            splitPaymentService.createSplit(request)
        );

        assertEquals("Incorrect transaction PIN", exception.getMessage());
    }

    @Test
    void testPayShare_Success() {
        SplitPayment split = new SplitPayment();
        split.setId(10L);
        split.setCreatedByUser(creator);
        split.setTotalAmount(new BigDecimal("100.00"));

        SplitPaymentParticipant pEntity = new SplitPaymentParticipant();
        pEntity.setSplit(split);
        pEntity.setUser(participant);
        pEntity.setAmountOwed(new BigDecimal("50.00"));
        pEntity.setPaid(false);

        when(authentication.getName()).thenReturn("participant@example.com");
        when(userRepository.findByEmail("participant@example.com")).thenReturn(Optional.of(participant));
        when(splitPaymentRepository.findById(10L)).thenReturn(Optional.of(split));
        when(participantRepository.findBySplit(split)).thenReturn(Collections.singletonList(pEntity));

        Wallet pWallet = new Wallet();
        pWallet.setBalance(new BigDecimal("100.00"));
        Wallet cWallet = new Wallet();
        cWallet.setBalance(new BigDecimal("200.00"));

        participant.setMtPin("encoded-participant-pin");
        when(walletRepository.findByUser(participant)).thenReturn(Optional.of(pWallet));
        when(walletRepository.findByUser(creator)).thenReturn(Optional.of(cWallet));
        when(passwordEncoder.matches("1234", "encoded-participant-pin")).thenReturn(true);

        splitPaymentService.payShare(10L, "1234");

        assertTrue(pEntity.isPaid());
        assertEquals(new BigDecimal("50.00"), pWallet.getBalance());
        assertEquals(new BigDecimal("250.00"), cWallet.getBalance());
        assertEquals("SETTLED", split.getStatus());
        
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(notificationService, times(1)).sendNotification(eq(creator), eq(NotificationType.TRANSACTION_RECEIVED), anyString());
    }

    @Test
    void testPayShare_IncorrectPin() {
        SplitPayment split = new SplitPayment();
        split.setId(10L);

        when(authentication.getName()).thenReturn("participant@example.com");
        when(userRepository.findByEmail("participant@example.com")).thenReturn(Optional.of(participant));
        when(splitPaymentRepository.findById(10L)).thenReturn(Optional.of(split));
        
        when(passwordEncoder.matches(eq("wrong"), any())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            splitPaymentService.payShare(10L, "wrong")
        );

        assertEquals("Incorrect transaction PIN", exception.getMessage());
    }
}
