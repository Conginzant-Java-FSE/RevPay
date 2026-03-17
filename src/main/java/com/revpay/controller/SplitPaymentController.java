package com.revpay.controller;

import com.revpay.dto.CreateSplitRequest;
import com.revpay.dto.SplitPaymentResponse;
import com.revpay.service.SplitPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/splits")
@Tag(name = "Split Payments", description = "Work with group splits and shared expenses")
@SecurityRequirement(name = "bearerAuth")
public class SplitPaymentController {

    @Autowired
    private SplitPaymentService splitPaymentService;

    @PostMapping("/create")
    @Operation(summary = "Create a new split")
    public ResponseEntity<SplitPaymentResponse> createSplit(@RequestBody CreateSplitRequest request) {
        return ResponseEntity.ok(splitPaymentService.createSplit(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get splits created by me")
    public ResponseEntity<List<SplitPaymentResponse>> getMySplits() {
        return ResponseEntity.ok(splitPaymentService.getMySplits());
    }

    @GetMapping("/owed")
    @Operation(summary = "Get splits where I owe money")
    public ResponseEntity<List<SplitPaymentResponse>> getOwedSplits() {
        return ResponseEntity.ok(splitPaymentService.getOwedSplits());
    }

    @PostMapping("/{splitId}/pay")
    @Operation(summary = "Pay my share of a split")
    public ResponseEntity<?> payShare(@PathVariable Long splitId, @RequestBody java.util.Map<String, String> request) {
        String pin = request.get("pin");
        splitPaymentService.payShare(splitId, pin);
        return ResponseEntity.ok().build();
    }
}
