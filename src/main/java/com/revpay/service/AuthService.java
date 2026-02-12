package com.revpay.service;

import com.revpay.model.BlacklistedToken;
import com.revpay.repository.BlacklistedTokenRepository;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class AuthService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public void logout(String token) {

        if (blacklistedTokenRepository.existsByToken(token)) {
            throw new RuntimeException("Token already invalidated");
        }

        try {
            Long userId = jwtUtil.extractUserId(token);
            Date expirationDate = jwtUtil.extractExpiration(token);

            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            BlacklistedToken blacklistedToken = new BlacklistedToken(
                    token,
                    LocalDateTime.now(),
                    expiresAt,
                    userId
            );

            blacklistedTokenRepository.save(blacklistedToken);

        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    // Scheduled task to clean up expired blacklisted tokens (runs daily at 2 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        System.out.println("Cleaned up expired blacklisted tokens at: " + LocalDateTime.now());
    }
}