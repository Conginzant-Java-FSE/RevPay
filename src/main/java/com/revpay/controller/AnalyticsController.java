package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Business Analytics", description = "Business analytics APIs — Business accounts only")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // ── 1. Summary
    @Operation(
            summary = "Business Analytics Summary",
            description = "High-level financial summary — total received, sent, and pending amounts."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/summary")
    public ResponseEntity<ApiDataResponse<AnalyticsSummaryResponse>> getSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        AnalyticsSummaryResponse data = analyticsService.getSummary(from, to);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Summary fetched successfully", data));
    }

    // ── 2. Revenue
    @Operation(
            summary = "Revenue Over Time",
            description = "Time-bucketed revenue data — DAILY, WEEKLY, or MONTHLY."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/revenue")
    public ResponseEntity<ApiDataResponse<List<RevenueDataResponse>>> getRevenue(
            @RequestParam String period, // DAILY | WEEKLY | MONTHLY

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<RevenueDataResponse> data = analyticsService.getRevenue(period, from, to);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Revenue data fetched successfully", data));
    }

    // ── 3. Top Customers
    @Operation(
            summary = "Top Customers",
            description = "Top N customers ranked by total transaction volume."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/top-customers")
    public ResponseEntity<ApiDataResponse<List<TopCustomerResponse>>> getTopCustomers(
            @RequestParam(defaultValue = "5") int limit,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        int safeLimit = Math.min(limit, 20);

        List<TopCustomerResponse> data = analyticsService.getTopCustomers(safeLimit, from, to);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Top customers fetched successfully", data));
    }

    // ── 4. Payment Trends
    @Operation(
            summary = "Payment Trends",
            description = "Daily incoming vs outgoing amounts for the cash flow trend chart."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/payment-trends")
    public ResponseEntity<ApiDataResponse<List<PaymentTrendResponse>>> getPaymentTrends(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<PaymentTrendResponse> data = analyticsService.getPaymentTrends(from, to);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Payment trends fetched successfully", data));
    }
}