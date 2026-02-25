package com.revpay.controller;

import com.revpay.dto.ApiResponse;
import com.revpay.dto.MoneyRequestCreateRequest;
import com.revpay.model.MoneyRequest;
import com.revpay.service.MoneyRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/money/requests")
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    public MoneyRequestController(MoneyRequestService moneyRequestService) {
        this.moneyRequestService = moneyRequestService;
    }
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<?>> createRequest(
            @RequestBody MoneyRequestCreateRequest requestDto,
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
                new ApiResponse(true, "Money request sent")
        );
    }
    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse> cancelRequest(@PathVariable Long requestId,
                                                     Authentication authentication) {

        String email = authentication.getName();

        moneyRequestService.cancelRequest(requestId, email);

        return ResponseEntity.ok(
                new ApiResponse(true, "Request cancelled")
        );
    }
}
