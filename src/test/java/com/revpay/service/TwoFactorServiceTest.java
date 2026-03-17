package com.revpay.service;

import com.revpay.dto.LoginResponse;
import com.revpay.enums.AccountType;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TwoFactorService twoFactorService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setAccountType(AccountType.PERSONAL);
    }

    @Test
    void testGenerateAndSendOtp() {
        twoFactorService.generateAndSendOtp(user);

        assertNotNull(user.getOtpCode());
        assertEquals(6, user.getOtpCode().length());
        assertNotNull(user.getOtpExpiry());
        assertTrue(user.getOtpExpiry().isAfter(LocalDateTime.now()));

        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1)).sendOtpEmail(eq(user.getEmail()), eq(user.getFullName()), anyString());
    }

    @Test
    void testVerifyOtp_Success() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("mock-jwt-token");

        LoginResponse response = twoFactorService.verifyOtp("test@example.com", "123456");

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiry());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            twoFactorService.verifyOtp("test@example.com", "wrong-otp")
        );

        assertEquals("Invalid verification code", exception.getMessage());
    }

    @Test
    void testVerifyOtp_ExpiredOtp() {
        user.setOtpCode("123456");
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            twoFactorService.verifyOtp("test@example.com", "123456")
        );

        assertEquals("Verification code has expired", exception.getMessage());
    }

    @Test
    void testEnableTwoFactor() {
        twoFactorService.enableTwoFactor(user);
        assertTrue(user.isTwoFactorEnabled());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testDisableTwoFactor() {
        user.setTwoFactorEnabled(true);
        user.setOtpCode("123456");
        twoFactorService.disableTwoFactor(user);
        assertFalse(user.isTwoFactorEnabled());
        assertNull(user.getOtpCode());
        verify(userRepository, times(1)).save(user);
    }
}
