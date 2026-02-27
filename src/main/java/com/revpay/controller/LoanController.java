package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.enums.LoanStatus;
import com.revpay.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Business Loans", description = "Business loan application and repayment APIs")
public class LoanController {

    @Autowired
    private LoanService loanService;

    //List all loans
    @Operation(
            summary = "List Loan Applications",
            description = "List all loan applications for the business account with optional status filter."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loans fetched successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Business account only"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<ApiDataResponse<Page<LoanListResponse>>> getLoans(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LoanListResponse> data = loanService.getLoans(status, pageable);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Loans fetched successfully", data));
    }

    // Apply for loan
    @Operation(
            summary = "Apply for Loan",
            description = "Submit a new loan application. Auto-approves if amount is under 50,000."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loan application submitted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or active loan exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Business account only"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/apply")
    public ResponseEntity<ApiDataResponse<LoanApplicationResponse>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request) {

        LoanApplicationResponse data = loanService.applyForLoan(request);

        return ResponseEntity.ok(
                new ApiDataResponse<>(true, "Loan application submitted successfully", data));
    }

    //Get loan details
    @Operation(
            summary = "Get Loan Details",
            description = "Get full loan details including EMI, interest, and repayment info."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Loan details fetched"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Loan not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{loanId}")
    public ResponseEntity<ApiDataResponse<LoanDetailResponse>> getLoanDetail(
            @PathVariable Long loanId) {

        LoanDetailResponse data = loanService.getLoanDetail(loanId);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Loan details fetched successfully", data));
    }

    // Repayment schedule
    @Operation(
            summary = "Get Repayment Schedule",
            description = "Get the full EMI repayment schedule with per-instalment breakdown."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repayment schedule fetched"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Loan not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{loanId}/repayment-schedule")
    public ResponseEntity<ApiDataResponse<List<RepaymentScheduleResponse>>> getRepaymentSchedule(
            @PathVariable Long loanId) {

        List<RepaymentScheduleResponse> data = loanService.getRepaymentSchedule(loanId);

        return ResponseEntity.ok(
                new ApiDataResponse<>(true, "Repayment schedule fetched successfully", data));
    }

    //Make repayment
    @Operation(
            summary = "Make Loan Repayment",
            description = "Make a repayment against an active loan from wallet balance."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Repayment successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient wallet balance or amount below EMI"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Loan not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{loanId}/repay")
    public ResponseEntity<ApiDataResponse<LoanRepaymentResponse>> repayLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody LoanRepaymentRequest request) {

        LoanRepaymentResponse data = loanService.repayLoan(loanId, request);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Repayment successful", data));
    }
}