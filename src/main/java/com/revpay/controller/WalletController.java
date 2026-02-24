package com.revpay.controller;

import com.revpay.dto.AddFundsRequest;
import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.ApiResponse;
import com.revpay.dto.BankAccountResponse;
import com.revpay.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
@Tag(name = "Wallet Management", description = "APIs for Wallet Management")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Operation(
            summary = "Add Funds to Wallet",
            description = "Simulates a card charge and credits the wallet. Creates a TOPUP transaction record."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Funds added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount, card not found, or incorrect PIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PostMapping("/add-funds")
    public ResponseEntity<ApiResponse<Void>> addFunds(@Valid @RequestBody AddFundsRequest request) {

        walletService.addFunds(request);

        ApiResponse<Void> response = new ApiResponse<>(true, "Funds added successfully");

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Get Linked Bank Account",
            description = "Fetches the primary linked bank account for withdrawals. Account number is masked."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/bank-account")
    public ResponseEntity<ApiDataResponse<BankAccountResponse>> getLinkedBankAccount() {

        BankAccountResponse data = walletService.getLinkedBankAccount();

        ApiDataResponse<BankAccountResponse> response =
                new ApiDataResponse<>(
                        true,
                        "Bank account fetched successfully",
                        data
                );

        return ResponseEntity.ok(response);
    }

}
