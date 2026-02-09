package com.revpay.service;
import com.revpay.dto.LoginRequest;
import com.revpay.dto.LoginResponse;
import com.revpay.dto.UserRegistrationRequest;
import com.revpay.enums.AccountType;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Existing register method
    public String register(UserRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already registered";
        }

        // Check if phone already exists
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return "Phone number already registered";
        }

        // Create new user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer()));
        user.setAccountType(request.getAccountType() != null ? request.getAccountType() : AccountType.PERSONAL);
        user.setActive(true);

        userRepository.save(user);

        return "User registered successfully";
    }

    // NEW: Login method
    public LoginResponse login(LoginRequest request) {
        // Find user by email or phone
        User user = (User) userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if account is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getAccountType().toString()
        );

        // Return login response
        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAccountType()
        );
    }
}