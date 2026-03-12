
package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.*;
import com.revpay.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

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
        emailService.sendWelcomeEmail(request.getEmail(), request.getFullName());

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

    @Operation(
            summary = "Logout User",
            description = "Invalidate the JWT token by blacklisting it"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LogoutResponse.class)
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid authorization header or token already invalidated",
                    content = @Content
            ),

            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired token",
                    content = @Content
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract token from Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Blacklist the token
            authService.logout(token);

            LogoutResponse response = new LogoutResponse("Logout successful", true);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "All Registered User", description = "Fetch all registered users with their details..")
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

    @Operation(
            summary = "Verify User Identity",
            description = "Verifies user by email or phone and returns the configured security question"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Security question fetched successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = VerifyIdentityResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Account inactive", content = @Content)
    })
    @PostMapping("/forgot-password/verify-identity")
    public ResponseEntity<VerifyIdentityResponse> verifyIdentity(
            @RequestBody VerifyIdentityRequest request) {

        VerifyIdentityResponse response = authService.verifyIdentity(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Validate Security Answer",
            description = "Validates the security question and answer and generates a short-lived reset token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Security answer validated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidateSecurityResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Security question or answer incorrect", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PostMapping("/forgot-password/validate-security")
    public ResponseEntity<ValidateSecurityResponse> validateSecurity(
            @RequestBody ValidateSecurityRequest request) {

        ValidateSecurityResponse response = authService.validateSecurity(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset Password",
            description = "Resets the user password using a short-lived reset token"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired reset token", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content
            )
    })
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<com.revpay.dto.ApiResponse<Void>> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        com.revpay.dto.ApiResponse<Void> response =
                new com.revpay.dto.ApiResponse<>(true, "Password reset successfully");

        return ResponseEntity.ok(response);
    }

}
