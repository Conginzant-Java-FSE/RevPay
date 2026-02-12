package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "APIs for user registration, login, and password management")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

//    @Autowired
//    private JwtUtil jwtUtil;

    @Operation(summary = "Register a new user", description = "Registers a new user or business user with encrypted password and security question")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "409", description = "Email or phone already registered"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {

        UserRegistrationResponse response = userService.register(request);

        if (response.getMessage().contains("already")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login user", description = "Authenticates a user with email/phone and password, returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or inactive account")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    //    @GetMapping("/profile")
    //    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String token) {
    //        try {
    //            // Extract userId from JWT token
    //
    //            Long userId = jwtUtil.extractUserId(token.substring(7)); // Remove "Bearer " prefix
    //            ProfileResponse profile = userService.getProfile(userId);
    //            return ResponseEntity.ok(profile);
    //        } catch (RuntimeException e) {
    //            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
    //                    .body(Map.of("error", e.getMessage()));
    //        }
    //    }

    @Operation(summary = "User Profile Details", description = "Fetched user details by verifying the token passed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Details Fetched Successfully", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String email = authentication.getName();
        ProfileResponse profile = userService.getProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<UserListResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Forgot password", description = "Resets the user's password after verifying their security question and answer. The new password is stored in encrypted format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content(schema = @Schema(implementation = ForgotPasswordResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found with provided email/phone"),
            @ApiResponse(responseCode = "401", description = "Security question or answer does not match"),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request body")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Received forgot-password request for: {}", request.getEmailOrPhone());
        ForgotPasswordResponse response = userService.forgotPassword(request);
        logger.info("Password reset completed for: {}", request.getEmailOrPhone());
        return ResponseEntity.ok(response);
    }
}
