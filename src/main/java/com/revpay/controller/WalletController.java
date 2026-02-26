package com.revpay.controller;

import com.revpay.dto.*;
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
                summary = "Get Wallet Balance",
                description = "Returns the current wallet balance and currency"
        )
        @SecurityRequirement(name = "bearerAuth")
        @GetMapping("/balance")
        public ResponseEntity<ApiDataResponse<WalletBalanceResponse>> getWalletBalance() {

                WalletBalanceResponse data = walletService.getWalletBalance();

                ApiDataResponse<WalletBalanceResponse> response =
                        new ApiDataResponse<>(true, "Wallet balance fetched successfully", data);

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

        @Operation(
                summary = "Update Bank Account",
                description = "Update the linked bank account for withdrawals. Requires password confirmation."
        )
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bank account updated successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Incorrect password or validation error"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Bank account not found")
        })
        @PutMapping("/bank-account/update")
        public ResponseEntity<ApiResponse<Void>> updateBankAccount(
                @Valid @RequestBody UpdateBankAccountRequest request) {

                walletService.updateBankAccount(request);

                ApiResponse<Void> response = new ApiResponse<>(true, "Bank account updated successfully");

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Withdraw Funds from Wallet", description = "Simulates a withdrawal from the wallet to the linked primary bank account. Creates a WITHDRAW transaction record.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Withdrawal completed successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid amount, insufficient balance, or incorrect PIN"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
        })
        @PostMapping("/withdraw")
        public ResponseEntity<ApiResponse<Void>> withdrawFunds(@Valid @RequestBody WithdrawRequest request) {

                walletService.withdrawFunds(request);

                ApiResponse<Void> response = new ApiResponse<>(true, "Operation completed successfully");

                return ResponseEntity.ok(response);
        }

}