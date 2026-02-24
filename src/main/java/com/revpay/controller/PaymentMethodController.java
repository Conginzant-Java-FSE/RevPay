package com.revpay.controller;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.CardResponseDTO;
import com.revpay.service.PaymentMethodService;
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

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/add")
    public ResponseEntity<ApiDataResponse<CardResponseDTO>> addCard(
            @RequestBody AddCardRequest request) {

        CardResponseDTO response = paymentMethodService.addCard(request);

        return ResponseEntity.ok(
                new ApiDataResponse<>(
                        true,
                        "Card added successfully",
                        response
                )
        );
    }
}