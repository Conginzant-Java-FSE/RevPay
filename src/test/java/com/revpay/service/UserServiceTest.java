package com.revpay.service;

import com.revpay.dto.ForgotPasswordRequest;
import com.revpay.dto.ForgotPasswordResponse;
import com.revpay.exception.SecurityAnswerMismatchException;
import com.revpay.exception.UserNotFoundException;
import com.revpay.model.User;
import com.revpay.enums.AccountType;
import com.revpay.repository.UserRepository;
import com.revpay.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private ForgotPasswordRequest validRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPhone("9876543210");
        testUser.setPassword("$2a$10$encodedOldPassword");
        testUser.setSecurityQuestion("What is your pet's name?");
        testUser.setSecurityAnswer("$2a$10$encodedSecurityAnswer");
        testUser.setAccountType(AccountType.PERSONAL);
        testUser.setActive(true);

        validRequest = new ForgotPasswordRequest();
        validRequest.setEmailOrPhone("john@example.com");
        validRequest.setSecurityQuestion("What is your pet's name?");
        validRequest.setSecurityAnswer("Buddy");
        validRequest.setNewPassword("NewSecure@123");
    }

    @Test
    @DisplayName("Forgot password - success with email")
    void forgotPassword_withValidEmail_shouldResetPassword() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Buddy", "$2a$10$encodedSecurityAnswer")).thenReturn(true);
        when(passwordEncoder.encode("NewSecure@123")).thenReturn("$2a$10$encodedNewPassword");

        ForgotPasswordResponse response = userService.forgotPassword(validRequest);

        assertTrue(response.isSuccess());
        assertEquals("Password reset successfully", response.getMessage());
        verify(userRepository).save(testUser);
        verify(passwordEncoder).encode("NewSecure@123");
        assertEquals("$2a$10$encodedNewPassword", testUser.getPassword());
    }

    @Test
    @DisplayName("Forgot password - user not found")
    void forgotPassword_userNotFound_shouldThrowUserNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("unknown@example.com")).thenReturn(Optional.empty());
        validRequest.setEmailOrPhone("unknown@example.com");

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.forgotPassword(validRequest));

        assertEquals("No user found with the provided email or phone number", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Forgot password - security answer mismatch")
    void forgotPassword_wrongSecurityAnswer_shouldThrowSecurityAnswerMismatchException() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Buddy", "$2a$10$encodedSecurityAnswer")).thenReturn(false);

        SecurityAnswerMismatchException exception = assertThrows(SecurityAnswerMismatchException.class,
                () -> userService.forgotPassword(validRequest));

        assertEquals("Security answer is incorrect", exception.getMessage());
        verify(userRepository, never()).save(any());
    }
}
