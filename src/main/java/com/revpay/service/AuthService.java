package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.enums.NotificationType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private NotificationRepository notificationRepository;

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

    public VerifyIdentityResponse verifyIdentity(VerifyIdentityRequest request) {

        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account inactive");
        }

        return new VerifyIdentityResponse(true, "Identity verified successfully", user.getSecurityQuestion());
    }

    public ValidateSecurityResponse validateSecurity(
            ValidateSecurityRequest request) {

        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getSecurityQuestion()
                .equalsIgnoreCase(request.getSecurityQuestion())) {

            throw new SecurityAnswerMismatchException("Security question mismatch");
        }

        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            throw new SecurityAnswerMismatchException("Security answer incorrect");
        }

        //Generate short-lived reset token
        String resetToken = jwtUtil.generateResetToken(user.getId());

        return new ValidateSecurityResponse(true, "Security validated successfully", resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        Claims claims;

        try {
            claims = jwtUtil.extractAllClaims(request.getResetToken());
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        if (!"RESET_PASSWORD".equals(claims.getSubject())) {
            throw new RuntimeException("Invalid reset token");
        }

        Long userId = claims.get("userId", Long.class);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Create notification
        createPasswordChangedNotification(user);
    }

    // Password updated notification
    private void createPasswordChangedNotification(User user) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Your password was changed successfully. If this wasn't you, please contact support immediately.");
        notification.setType(NotificationType.SECURITY_ALERT);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        logger.info("Password change notification created for: {}", user.getEmail());
    }

}