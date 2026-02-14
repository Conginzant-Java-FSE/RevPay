package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
@Tag(name = "User Management", description = "APIs for User Management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "User Profile Details", description = "Fetched user details by verifying the token passed.")
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

}