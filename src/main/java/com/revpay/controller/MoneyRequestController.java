package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.model.MoneyRequest;
import com.revpay.service.MoneyRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/money/requests")
@Tag(name = "Money Requests", description = "Send, accept, decline and cancel money requests")
public class MoneyRequestController {

    @Autowired
    private MoneyRequestService moneyRequestService;

    @Operation(
            summary = "Money Request",
            description = "Request money from a user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/request")
    public ResponseEntity<ApiDataResponse<?>> createRequest(@RequestBody MoneyRequestCreateRequest requestDto,
            Authentication authentication) {

        String email = authentication.getName();

        MoneyRequest request = moneyRequestService.createRequest(requestDto, email);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", request.getRequestId());
        data.put("amount", request.getAmount());
        data.put("purpose", request.getPurpose());
        data.put("status", request.getStatus());
        data.put("expiresAt", request.getExpiresAt());

        return ResponseEntity.ok(
                new ApiDataResponse(true, "Money request sent", data)
        );
    }

    @Operation(
            summary = "Cancel Money Request",
            description = "Cancel the requested money from a user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse> cancelRequest(@PathVariable Long requestId, Authentication authentication) {

        String email = authentication.getName();

        moneyRequestService.cancelRequest(requestId, email);

        return ResponseEntity.ok(
                new ApiResponse(true, "Request cancelled")
        );
    }

    @Operation(
            summary = "Decline Money Request",
            description = "Decline an incoming money request. No funds move. Requester is notified."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request declined"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found or does not belong to this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request is not in PENDING status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/{requestId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineRequest(@PathVariable Long requestId) {

        moneyRequestService.declineRequest(requestId);

        ApiResponse<Void> response = new ApiResponse<>(true, "Request declined");

        return ResponseEntity.ok(response);
    }

    //Incoming requests
    @Operation(
            summary = "Incoming Money Requests",
            description = "List all pending money requests directed at the logged-in user."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/incoming")
    public ResponseEntity<ApiDataResponse<Page<IncomingRequestResponse>>> getIncomingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<IncomingRequestResponse> data = moneyRequestService.getIncomingRequests(pageable);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Incoming requests fetched successfully", data));
    }

    //Outgoing requests
    @Operation(
            summary = "Outgoing Money Requests",
            description = "List all money requests sent by the logged-in user — all statuses."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/outgoing")
    public ResponseEntity<ApiDataResponse<Page<OutgoingRequestResponse>>> getOutgoingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<OutgoingRequestResponse> data = moneyRequestService.getOutgoingRequests(pageable);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Outgoing requests fetched successfully", data));
    }

    // Accept money request
    @Operation(
            summary = "Accept Money Request",
            description = "Accept an incoming money request. Debits the logged-in user and credits the requester atomically."
    )
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Request accepted and payment processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Insufficient balance, incorrect PIN, or request not pending"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Request not found or does not belong to this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{requestId}/accept")
    public ResponseEntity<ApiDataResponse<AcceptMoneyRequestResponse>> acceptRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody AcceptMoneyRequestRequest request) {

        AcceptMoneyRequestResponse data = moneyRequestService.acceptRequest(requestId, request);

        return ResponseEntity.ok(
                new ApiDataResponse<>(true, "Request accepted and payment processed", data));
    }
}
