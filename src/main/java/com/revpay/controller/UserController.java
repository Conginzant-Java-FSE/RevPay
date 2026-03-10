package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "APIs for User Management")
public class UserController {

    private final UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "User Profile Details", description = "Fetched user details by verifying the token passed.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Details Fetched Successfully", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        String email = authentication.getName();
        ProfileResponse profile = userService.getProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "User Profile Creation", description = "Create the users profile after login.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile created Successfully", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Personal profile already exists for this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PostMapping("/create-personal-user")
    public ResponseEntity<ApiResponse<Void>> createPersonalProfileWithBank(
            @RequestBody PersonalProfileFullRequest request) {

        userService.createPersonalProfileWithBank(request);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Personal profile and bank account created successfully");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update Personal Profile",
            description = "Update the logged-in user's personal profile and bank details"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Profile does not exist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/update-personal-user")
    public ResponseEntity<ApiResponse<Void>> updatePersonalProfileWithBank(
            @RequestBody PersonalProfileFullRequest request) {

        logger.info("Update profile request received");

        userService.updatePersonalProfileWithBank(request);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Personal profile and bank account updated successfully");

        logger.info("Profile updated successfully");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set Transaction PIN", description = "Set Money Transaction PIN for logged-in user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Transaction PIN set successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "PIN validation failed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PostMapping("/set-mt-pin")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(
            @Valid @RequestBody SetTransactionPinRequest request) {

        userService.setTransactionPin(request);

        ApiResponse<Void> response = new ApiResponse<>(true, "Transaction PIN set successfully");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Business Profile Creation", description = "Create the users business profile after login.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Business profile created Successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Business profile already exists for this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PostMapping("/create-business-profile")
    public ResponseEntity<ApiResponse<Void>> createBusinessProfileWithBank(
            @Valid @RequestBody BusinessProfileFullRequest request) {

        userService.createBusinessProfileWithBank(request);

        ApiResponse<Void> response = new ApiResponse<>(true,
                "Business profile and bank account created successfully");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get Security Question",
            description = "Returns the security question the logged-in user set during registration."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security question retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/security-question")
    public ResponseEntity<ApiDataResponse<SecurityQuestionResponse>> getSecurityQuestion() {

        SecurityQuestionResponse data = userService.getSecurityQuestion();

        ApiDataResponse<SecurityQuestionResponse> response =
                new ApiDataResponse<>(true, "Security question retrieved successfully", data);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get Full Profile",
            description = "Returns the complete profile of the authenticated user including wallet balance and bank account. Used to populate the dashboard and profile page."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiDataResponse<ProfileMeResponse>> getFullProfile() {

        ProfileMeResponse data = userService.getFullProfile();

        ApiDataResponse<ProfileMeResponse> response =
                new ApiDataResponse<>(true, "Profile fetched successfully", data);

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Update Business Profile",
            description = "Allows business users to update their business details"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile/business/update")
    public ResponseEntity<ApiResponse<Void>> updateBusinessProfile(
            @RequestBody BusinessProfileUpdateRequest request) {

        userService.updateBusinessProfile(request);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Operation completed successfully");

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Change Password",
            description = "Allows an authenticated user to change their password. " +
                    "Requires the current password for verification before applying the new one."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Current password is incorrect / passwords do not match / validation error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing token"
            )
    })
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Operation completed successfully");

        return ResponseEntity.ok(response);
    }
}