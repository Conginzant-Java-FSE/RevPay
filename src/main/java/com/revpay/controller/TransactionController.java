package com.revpay.controller;

import com.revpay.dto.ChatTransactionDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.revpay.service.TransactionService;
import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.SendMoneyRequest;
import com.revpay.dto.TransactionResponseDTO;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@Tag(name = "Transaction Management", description = "APIs for sending money, transaction history, and analytics")
public class TransactionController {

        @Autowired
        private TransactionService transactionService;

        @Operation(summary = "Get Transaction History", description = """
                        Paginated transaction history with optional combined filters.
                        All query params are optional and fully combinable.

                        **type values:** SEND | RECEIVE | REQUEST | ADD_MONEY | WITHDRAW | LOAN_REPAYMENT

                        **status values:** PENDING | SUCCESS | FAILED | CANCELLED

                        **Date format:** yyyy-MM-dd

                        Default: page=0, size=10 | Max size: 100
                        """)
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History fetched successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping
        public ResponseEntity<Map<String, Object>> getTransactions(

                        @Parameter(description = "Page number (zero-based). Default: 0") @RequestParam(defaultValue = "0") int page,

                        @Parameter(description = "Page size. Default: 10, Max: 100") @RequestParam(defaultValue = "10") int size,

                        @Parameter(description = "Filter by type: SEND | REQUEST | ADD_MONEY | WITHDRAW | LOAN_REPAYMENT") @RequestParam(required = false) TransactionType type,

                        @Parameter(description = "Filter by status: PENDING | SUCCESS | FAILED | CANCELLED") @RequestParam(required = false) TransactionStatus status,

                        @Parameter(description = "Start date inclusive. Format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @Parameter(description = "End date inclusive. Format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

                        @Parameter(description = "Search by counterparty name, email, or transaction ID") @RequestParam(required = false) String search

        ) {
                Map<String, Object> response = transactionService.getTransactions(page, size, type, status, from, to,
                                search);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get Transaction Detail", description = "Fetch full details of a single transaction by its ID. "
                        +
                        "Only returns the transaction if it belongs to the authenticated user.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Transaction not found or access denied"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/{transactionId}")
        public ResponseEntity<ApiDataResponse<TransactionResponseDTO>> getTransactionById(
                        @PathVariable Long transactionId) {

                TransactionResponseDTO data = transactionService.getTransactionById(transactionId);

                return ResponseEntity.ok(
                                new ApiDataResponse<>(true, "Transaction fetched successfully", data));
        }

        @Operation(summary = "Send Money", description = """
                        Transfer money from your wallet to another user's wallet.

                        - Receiver identified by **email or phone number**
                        - Requires your **4-digit transaction PIN**
                        - Deducts from sender's wallet, credits receiver's wallet atomically
                        - Both parties receive a notification
                        - Low balance warning triggered if sender balance drops below ₹100
                        """)
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Money sent successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance / incorrect PIN / receiver not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @PostMapping("/send")
        public ResponseEntity<ApiDataResponse<TransactionResponseDTO>> sendMoney(
                        @Valid @RequestBody SendMoneyRequest request) {

                TransactionResponseDTO data = transactionService.sendMoney(request);

                return ResponseEntity.ok(
                                new ApiDataResponse<>(true, "Money sent successfully", data));
        }

        @Operation(summary = "Get Recent Transactions", description = "Returns the last 5 transactions for the authenticated user. "
                        +
                        "Designed for dashboard widgets.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recent transactions fetched"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/recent")
        public ResponseEntity<ApiDataResponse<List<TransactionResponseDTO>>> getRecentTransactions() {

                List<TransactionResponseDTO> data = transactionService.getRecentTransactions();

                return ResponseEntity.ok(
                                new ApiDataResponse<>(true, "Recent transactions fetched successfully", data));
        }

        @Operation(summary = "Get Transaction Summary", description = """
                        Returns aggregated analytics for the authenticated user.

                        Includes: total sent, total received, total top-ups, net flow,
                        transaction counts by status (SUCCESS / FAILED / PENDING).

                        Defaults to **current month** if no date range is provided.
                        """)
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary fetched successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/summary")
        public ResponseEntity<Map<String, Object>> getTransactionSummary(

                        @Parameter(description = "Start date. Format: yyyy-MM-dd. Default: first day of current month") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @Parameter(description = "End date. Format: yyyy-MM-dd. Default: today") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to

        ) {
                Map<String, Object> response = transactionService.getTransactionSummary(from, to);
                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Export Transaction History", description = "Export filtered transaction history as a downloadable CSV or PDF file. "
                        +
                        "Use format=CSV or format=PDF.")
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File exported successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid format"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
        })
        @GetMapping("/export")
        public void exportTransactions(
                        @Parameter(description = "Export format: CSV or PDF", required = true) @RequestParam String format,

                        @Parameter(description = "Filter by type: SEND | REQUEST | ADD_MONEY | WITHDRAW | LOAN_REPAYMENT") @RequestParam(required = false) TransactionType type,

                        @Parameter(description = "Filter by status: PENDING | SUCCESS | FAILED | CANCELLED") @RequestParam(required = false) TransactionStatus status,

                        @Parameter(description = "Start date inclusive. Format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

                        @Parameter(description = "End date inclusive. Format: yyyy-MM-dd") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

                        @Parameter(description = "Search by counterparty name, email, or transaction ID") @RequestParam(required = false) String search,

                        HttpServletResponse response) throws IOException {

                transactionService.exportTransactions(format, type, status, from, to, search, response);
        }

        @Operation(
                summary = "Get Chat-Style Transaction History",
                description = """
        Returns all transactions between the authenticated user and another user,
        sorted by timestamp ASC (oldest first, like a chat conversation).
 
        Type from requesting user's perspective:
        - **PAID** → current user sent money
        - **RECEIVED** → current user received money
 
        The authenticated user's ID must match the {userId} path variable.
        """
        )
        @SecurityRequirement(name = "bearerAuth")
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chat history fetched successfully"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "User not found or access denied")
        })
        @GetMapping("/chat/{userId}/{otherUserId}")
        public ResponseEntity<ApiDataResponse<List<ChatTransactionDTO>>> getChatTransactionHistory(
                @PathVariable Long userId,
                @PathVariable Long otherUserId) {

                List<ChatTransactionDTO> data = transactionService.getChatTransactionHistory(userId, otherUserId);

                return ResponseEntity.ok(
                        new ApiDataResponse<>(true, "Chat transaction history fetched successfully", data)
                );
        }
}