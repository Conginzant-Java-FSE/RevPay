package com.revpay.controller;

import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.ApiResponse;
import com.revpay.dto.MoneyRequestCreateRequest;
import com.revpay.model.MoneyRequest;
import com.revpay.service.MoneyRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/money/requests")
@Tag(name = "Money Requests", description = "Send, accept, decline and cancel money requests")
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    public MoneyRequestController(MoneyRequestService moneyRequestService) {
        this.moneyRequestService = moneyRequestService;
    }

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
}
