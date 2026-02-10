package com.revpay.service;
import com.revpay.dto.*;
import com.revpay.enums.AccountType;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // New User Register
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new UserRegistrationResponse("Email already registered");
        }

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return new UserRegistrationResponse("Phone number already registered");
        }

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

        return new UserRegistrationResponse("User registered successfully");
    }

    // User Login method
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

    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProfileResponse profile = new ProfileResponse();
        profile.setUserId(user.getId());
        profile.setFullName(user.getFullName());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAccountType(user.getAccountType());
        //profile.setSecurityQuestion(user.getSecurityQuestion());

        return profile;
    }

    public List<UserListResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
            UserListResponse response = new UserListResponse();
            response.setUserId(user.getId());
            response.setFullName(user.getFullName());
            response.setEmail(user.getEmail());
            response.setPhone(user.getPhone());
            response.setAccountType(user.getAccountType());
            return response;
        }).collect(Collectors.toList());
    }
}