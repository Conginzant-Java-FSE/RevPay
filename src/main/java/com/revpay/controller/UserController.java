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

}