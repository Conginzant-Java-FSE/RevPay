package com.revpay.controller;


import com.revpay.dto.ApiResponse;
import com.revpay.dto.DeleteAccountRequest;
import com.revpay.service.ProfileService;
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
                    new ApiResponse(true, "Account deactivated successfully. We're sorry to see you go!")
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
}
