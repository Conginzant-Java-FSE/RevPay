package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.AuthService;
import com.revpay.service.UserService;
import com.revpay.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthService authService;

    @InjectMocks
    private AuthController authController;

    // ─── REGISTER

    @Test
    @DisplayName("register: should return 201 on successful registration")
    void register_validRequest_shouldReturn201() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("9876543210");
        request.setPassword("SecurePass@1");
        request.setSecurityQuestion("What is your pet's name?");
        request.setSecurityAnswer("Fluffy");

        UserRegistrationResponse response = new UserRegistrationResponse();
        response.setMessage("User registered successfully");

        when(userService.register(any(UserRegistrationRequest.class))).thenReturn(response);

        ResponseEntity<?> result = authController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((UserRegistrationResponse) result.getBody()).getMessage())
                .isEqualTo("User registered successfully");
    }

    @Test
    @DisplayName("register: should return 409 when email already in use")
    void register_duplicateEmail_shouldReturn409() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("john@example.com");

        UserRegistrationResponse conflictResponse = new UserRegistrationResponse();
        conflictResponse.setMessage("Email already registered");

        when(userService.register(any(UserRegistrationRequest.class))).thenReturn(conflictResponse);

        ResponseEntity<?> result = authController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(((UserRegistrationResponse) result.getBody()).getMessage())
                .isEqualTo("Email already registered");
    }

    // ─── LOGIN

    @Test
    @DisplayName("login: should return 200 with token on valid credentials")
    void login_validCredentials_shouldReturn200WithToken() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrPhone("john@example.com");
        request.setPassword("SecurePass@1");

        LoginResponse response = new LoginResponse();
        response.setToken("eyJhbGci.valid.jwt.token");
        response.setMessage("Login successful");

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        ResponseEntity<?> result = authController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((LoginResponse) result.getBody()).getToken()).isNotBlank();
        assertThat(((LoginResponse) result.getBody()).getMessage()).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("login: should return 401 when credentials are invalid")
    void login_invalidCredentials_shouldReturn401() {
        LoginRequest request = new LoginRequest();
        request.setEmailOrPhone("john@example.com");
        request.setPassword("WrongPassword");

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        ResponseEntity<?> result = authController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── VERIFY IDENTITY

    @Test
    @DisplayName("verifyIdentity: should return 200 with security question")
    void verifyIdentity_validUser_shouldReturn200() {
        VerifyIdentityRequest request = new VerifyIdentityRequest();
        request.setEmailOrPhone("john@example.com");

        VerifyIdentityResponse response = new VerifyIdentityResponse(
                true, "Identity verified", "What is your pet's name?");

        when(authService.verifyIdentity(any(VerifyIdentityRequest.class))).thenReturn(response);

        ResponseEntity<?> result = authController.verifyIdentity(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((VerifyIdentityResponse) result.getBody()).getSecurityQuestion())
                .isEqualTo("What is your pet's name?");
    }

}