package com.revpay.controller;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.ApiResponse;
import com.revpay.dto.CardResponseDTO;
import com.revpay.service.PaymentMethodService;
import com.revpay.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "*")
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
}