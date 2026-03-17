package com.revpay.service;

import com.revpay.dto.LoginResponse;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class TwoFactorService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public void generateAndSendOtp(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    @Transactional
    public LoginResponse verifyOtp(String emailOrPhone, String otp) {
        User user = (User) userRepository.findByEmail(emailOrPhone)
                .or(() -> userRepository.findByPhone(emailOrPhone))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Invalid verification code");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new RuntimeException("Verification code has expired");
        }

        // Clear OTP after successful verification
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getAccountType().toString());

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAccountType());
    }

    @Transactional
    public void enableTwoFactor(User user) {
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
    }
}
