package com.revpay.service;

import com.revpay.dto.AdminLoginRequest;
import com.revpay.dto.AdminLoginResponse;
import com.revpay.enums.AccountType;
import com.revpay.enums.Role;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminLoginResponse login(AdminLoginRequest request) {

        // 1. Find by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));

        // 2. Must be ADMIN
        if (user.getRole() != Role.ADMIN) {
            logger.warn("Non-admin login attempt: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid admin credentials");
        }

        // 3. Must be active
        if (!user.isActive()) {
            throw new IllegalStateException("Admin account is disabled");
        }
 
        // 4. Password check
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Wrong password for admin: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid admin credentials");
        }

        // 5. Generate token — uses your existing JwtUtil signature:
        //    generateToken(Long userId, String email, String accountType)
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getAccountType().name()  // "ADMIN"
        );

        logger.info("Admin login successful: {}", user.getEmail());

        return AdminLoginResponse.builder()
                .token(token)
                .adminId(String.valueOf(user.getId()))
                .email(user.getEmail())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                logger.info("Admin logout: {}", email);
            } catch (Exception ignored) {}
        }
    }
}