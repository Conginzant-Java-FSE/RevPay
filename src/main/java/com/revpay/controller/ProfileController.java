package com.revpay.controller;


import com.revpay.dto.*;
import com.revpay.service.*;
import com.revpay.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserService userService;

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody DeleteAccountRequest request) {

        try {

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid authorization header"));
            }

            String token = authHeader.substring(7);

            profileService.deleteAccount(token, request);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Account deactivated successfully. We're sorry to see you go!"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PutMapping("/change-pin")
    public ResponseEntity<ApiResponse<Void>> changePin(
            @Valid @RequestBody ChangePinRequest request) {

        userService.changeTransactionPin(request);

        ApiResponse<Void> response = new ApiResponse<>(true, "Operation completed successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/reactivate-account")
    public ResponseEntity<?> reactivateAccount(
            @Valid @RequestBody LoginRequest request) {

        try {

            profileService.reactivateAccount(request);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Account reactivated successfully. Welcome back!")
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @Operation(
            summary = "Update Security Question",
            description = "Change security question and answer. Current password required for verification."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Security question updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Incorrect current password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/security-question/update")
    public ResponseEntity<ApiResponse<Void>> updateSecurityQuestion(
            @Valid @RequestBody UpdateSecurityQuestionRequest request) {

        userService.updateSecurityQuestion(request);

        ApiResponse<Void> response = new ApiResponse<>(true, "Security question updated successfully");

        return ResponseEntity.ok(response);
    }
}
