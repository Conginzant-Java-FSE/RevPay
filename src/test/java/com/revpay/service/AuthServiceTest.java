package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.exception.SecurityAnswerMismatchException;
import com.revpay.exception.UserNotFoundException;
import com.revpay.model.BlacklistedToken;
import com.revpay.model.Notification;
import com.revpay.model.User;
import com.revpay.repository.BlacklistedTokenRepository;
import com.revpay.repository.NotificationRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private BlacklistedTokenRepository blacklistedTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setPhone("9876543210");
        mockUser.setFullName("John Doe");
        mockUser.setPassword("$2a$10$encodedPassword");
        mockUser.setSecurityQuestion("What is your pet's name?");
        mockUser.setSecurityAnswer("$2a$10$encodedAnswer");
        mockUser.setActive(true);
    }

    // LOGOUT

    @Test
    @DisplayName("logout: should blacklist a valid token successfully")
    void logout_validToken_shouldBlacklist() {
        String token = "valid.jwt.token";
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(false);
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractExpiration(token)).thenReturn(new Date(System.currentTimeMillis() + 60000));

        authService.logout(token);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(token);
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("logout: should throw when token is already blacklisted")
    void logout_alreadyBlacklisted_shouldThrow() {
        String token = "already.blacklisted.token";
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(true);

        assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token already invalidated");

        verify(blacklistedTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("logout: should throw RuntimeException on invalid token")
    void logout_invalidToken_shouldThrow() {
        String token = "bad.token";
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(false);
        when(jwtUtil.extractUserId(token)).thenThrow(new RuntimeException("JWT parse error"));

        assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid token");
    }

    // VERIFY IDENTITY

    @Test
    @DisplayName("verifyIdentity: should return security question for valid email")
    void verifyIdentity_validEmail_shouldReturnSecurityQuestion() {
        VerifyIdentityRequest request = new VerifyIdentityRequest();
        request.setEmailOrPhone("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        VerifyIdentityResponse response = authService.verifyIdentity(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSecurityQuestion()).isEqualTo("What is your pet's name?");
    }

    @Test
    @DisplayName("verifyIdentity: should work with phone number")
    void verifyIdentity_validPhone_shouldReturnSecurityQuestion() {
        VerifyIdentityRequest request = new VerifyIdentityRequest();
        request.setEmailOrPhone("9876543210");

        when(userRepository.findByEmail("9876543210")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(mockUser));

        VerifyIdentityResponse response = authService.verifyIdentity(request);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("verifyIdentity: should throw when user not found")
    void verifyIdentity_userNotFound_shouldThrow() {
        VerifyIdentityRequest request = new VerifyIdentityRequest();
        request.setEmailOrPhone("unknown@example.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyIdentity(request))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("verifyIdentity: should throw when account is inactive")
    void verifyIdentity_inactiveAccount_shouldThrow() {
        mockUser.setActive(false);
        VerifyIdentityRequest request = new VerifyIdentityRequest();
        request.setEmailOrPhone("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.verifyIdentity(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account inactive");
    }

    // VALIDATE SECURITY

    @Test
    @DisplayName("validateSecurity: should return reset token on correct answer")
    void validateSecurity_correctAnswer_shouldReturnResetToken() {
        ValidateSecurityRequest request = new ValidateSecurityRequest();
        request.setEmailOrPhone("john@example.com");
        request.setSecurityQuestion("What is your pet's name?");
        request.setSecurityAnswer("Fluffy");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("Fluffy", mockUser.getSecurityAnswer())).thenReturn(true);
        when(jwtUtil.generateResetToken(1L)).thenReturn("reset.token.value");

        ValidateSecurityResponse response = authService.validateSecurity(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResetToken()).isEqualTo("reset.token.value");
    }

    @Test
    @DisplayName("validateSecurity: should throw on wrong security question")
    void validateSecurity_wrongQuestion_shouldThrow() {
        ValidateSecurityRequest request = new ValidateSecurityRequest();
        request.setEmailOrPhone("john@example.com");
        request.setSecurityQuestion("Wrong question?");
        request.setSecurityAnswer("Fluffy");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> authService.validateSecurity(request))
                .isInstanceOf(SecurityAnswerMismatchException.class)
                .hasMessage("Security question mismatch");
    }

    @Test
    @DisplayName("validateSecurity: should throw on wrong security answer")
    void validateSecurity_wrongAnswer_shouldThrow() {
        ValidateSecurityRequest request = new ValidateSecurityRequest();
        request.setEmailOrPhone("john@example.com");
        request.setSecurityQuestion("What is your pet's name?");
        request.setSecurityAnswer("WrongAnswer");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("WrongAnswer", mockUser.getSecurityAnswer())).thenReturn(false);

        assertThatThrownBy(() -> authService.validateSecurity(request))
                .isInstanceOf(SecurityAnswerMismatchException.class)
                .hasMessage("Security answer incorrect");
    }

    //  RESET PASSWORD

    @Test
    @DisplayName("resetPassword: should update password with valid reset token")
    void resetPassword_validToken_shouldUpdatePassword() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("valid.reset.token");
        request.setNewPassword("NewSecurePass@1");

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("RESET_PASSWORD");
        when(mockClaims.get("userId", Long.class)).thenReturn(1L);

        when(jwtUtil.extractAllClaims("valid.reset.token")).thenReturn(mockClaims);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("NewSecurePass@1")).thenReturn("$2a$10$newEncoded");

        authService.resetPassword(request);

        verify(userRepository).save(mockUser);
        assertThat(mockUser.getPassword()).isEqualTo("$2a$10$newEncoded");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("resetPassword: should throw on invalid reset token")
    void resetPassword_invalidToken_shouldThrow() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("bad.token");
        request.setNewPassword("NewPass@1");

        when(jwtUtil.extractAllClaims("bad.token")).thenThrow(new RuntimeException("JWT parse error"));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid or expired reset token");
    }

    @Test
    @DisplayName("resetPassword: should throw when claims subject is not RESET_PASSWORD")
    void resetPassword_wrongSubject_shouldThrow() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("non.reset.token");
        request.setNewPassword("NewPass@1");

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("ACCESS_TOKEN");
        when(jwtUtil.extractAllClaims("non.reset.token")).thenReturn(mockClaims);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid reset token");
    }
}
