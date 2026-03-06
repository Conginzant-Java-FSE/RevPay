package com.revpay.util;

import com.revpay.repository.BlacklistedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private JwtUtil jwtUtil;

    private static final String SECRET = "ThisIsAVeryLongTestSecretKeyForJwtTesting1234567890";
    private static final Long EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION);
    }


    @Test
    @DisplayName("generateToken: should create a valid JWT with correct claims")
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken(1L, "john@example.com", "PERSONAL");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("john@example.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("extractUsername: should return email from valid token")
    void extractUsername_validToken_shouldReturnEmail() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "BUSINESS");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractUsername: should return null for malformed token")
    void extractUsername_invalidToken_shouldReturnNull() {
        String result = jwtUtil.extractUsername("not.a.valid.token");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("isTokenExpired: should return false for freshly generated token")
    void isTokenExpired_freshToken_shouldReturnFalse() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "PERSONAL");

        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }


    @Test
    @DisplayName("validateToken: should return true for valid, non-blacklisted token")
    void validateToken_validToken_shouldReturnTrue() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "PERSONAL");
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(false);

        assertThat(jwtUtil.validateToken(token, "user@example.com")).isTrue();
    }

    @Test
    @DisplayName("validateToken: should return false for blacklisted token")
    void validateToken_blacklistedToken_shouldReturnFalse() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "PERSONAL");
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(true);

        assertThat(jwtUtil.validateToken(token, "user@example.com")).isFalse();
    }

    @Test
    @DisplayName("validateToken: should return false when username does not match token")
    void validateToken_usernameMismatch_shouldReturnFalse() {
        String token = jwtUtil.generateToken(1L, "user@example.com", "PERSONAL");

        assertThat(jwtUtil.validateToken(token, "other@example.com")).isFalse();
    }

    @Test
    @DisplayName("validateToken: should return false for completely invalid token")
    void validateToken_invalidToken_shouldReturnFalse() {
        assertThat(jwtUtil.validateToken("garbage.token.here", "user@example.com")).isFalse();
    }


    @Test
    @DisplayName("generateResetToken: should create a short-lived reset token with correct subject")
    void generateResetToken_shouldHaveCorrectSubject() {
        String resetToken = jwtUtil.generateResetToken(99L);

        assertThat(resetToken).isNotBlank();
        assertThat(jwtUtil.extractAllClaims(resetToken).getSubject()).isEqualTo("RESET_PASSWORD");
        assertThat(jwtUtil.extractAllClaims(resetToken).get("userId", Long.class)).isEqualTo(99L);
    }

    @Test
    @DisplayName("isTokenBlacklisted: should delegate to repository")
    void isTokenBlacklisted_shouldCheckRepository() {
        String token = "some.test.token";
        when(blacklistedTokenRepository.existsByToken(token)).thenReturn(true);

        assertThat(jwtUtil.isTokenBlacklisted(token)).isTrue();
        verify(blacklistedTokenRepository).existsByToken(token);
    }
}