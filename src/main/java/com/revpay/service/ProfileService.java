package com.revpay.service;

import com.revpay.dto.DeleteAccountRequest;
import com.revpay.dto.LoginRequest;
import com.revpay.enums.UserStatus;
import com.revpay.model.User;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public void deleteAccount(String token, DeleteAccountRequest request) {

        Long userId = jwtUtil.extractUserId(token);


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw new RuntimeException("Account is already deactivated");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        user.setStatus(UserStatus.DEACTIVATED);
        user.setActive(false);
        user.setDeactivatedAt(LocalDateTime.now());

        if (request.getReason() != null && !request.getReason().isEmpty()) {
            user.setDeactivationReason(request.getReason());
        }

        userRepository.save(user);
    }

    // Optional: Method to reactivate account
//    @Transactional
//    public void reactivateAccount(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (user.getStatus() != UserStatus.DEACTIVATED) {
//            throw new RuntimeException("Account is not deactivated");
//        }
//
//        user.setStatus(UserStatus.ACTIVE);
//        user.setDeactivatedAt(null);
//        user.setActive(true);
//        user.setDeactivationReason(null);
//
//        userRepository.save(user);
//    }
    @Transactional
    public void reactivateAccount(LoginRequest request) {

        User user = userRepository
                .findByEmailOrPhone(request.getEmailOrPhone(), request.getEmailOrPhone())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() != UserStatus.DEACTIVATED) {
            throw new RuntimeException("Account is not deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setActive(true); // remove if you delete active field
        user.setDeactivatedAt(null);
        user.setActive(true);
        user.setDeactivationReason(null);

        userRepository.save(user);
    }
}