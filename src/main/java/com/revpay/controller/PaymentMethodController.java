package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.PaymentMethodService;
import com.revpay.service.WalletService;
import com.revpay.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "*")
@Tag(name = "Payment Method Management", description = "APIs for Payment Method Management")
public class PaymentMethodController {

    @Autowired
    private PaymentMethodService paymentMethodService;

    @Autowired
    private WalletService walletService;

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add")
    public ResponseEntity<ApiDataResponse<CardResponseDTO>> addCard(@RequestBody AddCardRequest request) {

        CardResponseDTO response = paymentMethodService.addCard(request);

        return ResponseEntity.ok(
                new ApiDataResponse<>(
                        true,
                        "Card added successfully",
                        response
                )
        );
    }

    @Operation(
            summary = "Set Default Payment Method",
            description = "Mark the specified card as the default payment method. Previously defaulted card is automatically unset."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default card updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found or does not belong to this account"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/{cardId}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultCard(@PathVariable Long cardId) {

        walletService.setDefaultCard(cardId);

        ApiResponse<Void> response = new ApiResponse<>(true, "Default card updated");

        return ResponseEntity.ok(response);
    }

    @Autowired
    private JwtUtil jwtUtil;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getUserPaymentMethods(@RequestHeader("Authorization") String token) {

        try {


            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String jwt = token.substring(7);


            if (jwtUtil.isTokenExpired(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token has expired"));
            }


            Long userId = jwtUtil.extractUserId(jwt);


            List<PaymentMethodListDTO> cards = paymentMethodService.getUserCards(userId);

            return ResponseEntity.ok(
                    new ApiDataResponse<>(
                            true,
                            "Payment methods fetched successfully",
                            cards
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token or session: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Update Card",
            description = "Update card nickname or billing address. Card number and expiry cannot be changed."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Card updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card not found or does not belong to this account"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/{cardId}/update")
    public ResponseEntity<ApiResponse<Void>> updateCard(@PathVariable Long cardId, @Valid @RequestBody UpdateCardRequest request) {

        paymentMethodService.updateCard(cardId, request);

        ApiResponse<Void> response = new ApiResponse<>(true, "Card updated successfully");

        return ResponseEntity.ok(response);
    }
}