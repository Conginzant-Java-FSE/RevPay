package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.enums.InvoiceStatus;
import com.revpay.service.InvoiceService;
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

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice management APIs — Business accounts only")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    //List invoices
    @Operation(summary = "List Invoices", description = "List all invoices with optional status filter and search.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiDataResponse<Page<InvoiceListResponse>>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<InvoiceListResponse> data = invoiceService.getInvoices(status, search, pageable);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoices fetched successfully", data));
    }

    //  Create invoice
    @Operation(summary = "Create Invoice", description = "Create a new invoice in DRAFT status with line items and customer info.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/create")
    public ResponseEntity<ApiDataResponse<InvoiceCreateResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceCreateResponse data = invoiceService.createInvoice(request);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice created successfully", data));
    }

    //  Get invoice detail
    @Operation(summary = "Get Invoice Detail", description = "Get complete details of a single invoice including all line items.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiDataResponse<InvoiceDetailResponse>> getInvoiceDetail(
            @PathVariable Long invoiceId) {

        InvoiceDetailResponse data = invoiceService.getInvoiceDetail(invoiceId);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice fetched successfully", data));
    }

    // Update invoice
    @Operation(summary = "Update Invoice", description = "Edit a draft invoice before sending. Cannot edit SENT or PAID invoices.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{invoiceId}/update")
    public ResponseEntity<ApiDataResponse<InvoiceUpdateResponse>> updateInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody UpdateInvoiceRequest request) {

        InvoiceUpdateResponse data = invoiceService.updateInvoice(invoiceId, request);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice updated successfully", data));
    }

    // Send invoice
    @Operation(summary = "Send Invoice", description = "Send the invoice to customer. Changes status from DRAFT to SENT.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{invoiceId}/send")
    public ResponseEntity<ApiDataResponse<InvoiceSentResponse>> sendInvoice(
            @PathVariable Long invoiceId) {

        InvoiceSentResponse data = invoiceService.sendInvoice(invoiceId);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice sent to customer", data));
    }

    // Mark as paid
    @Operation(summary = "Mark Invoice as Paid", description = "Manually mark an invoice as paid.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{invoiceId}/mark-paid")
    public ResponseEntity<ApiDataResponse<InvoicePaidResponse>> markInvoicePaid(
            @PathVariable Long invoiceId) {

        InvoicePaidResponse data = invoiceService.markInvoicePaid(invoiceId);

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice marked as paid", data));
    }

    // Cancel invoice
    @Operation(summary = "Cancel Invoice", description = "Cancel a draft or sent invoice. Cannot cancel a paid invoice.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{invoiceId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(
            @PathVariable Long invoiceId) {

        invoiceService.cancelInvoice(invoiceId);

        return ResponseEntity.ok(new ApiResponse<>(true, "Invoice cancelled"));
    }

    // Invoice summary
    @Operation(summary = "Invoice Summary",
            description = "Get aggregated invoice totals — paid, unpaid, and overdue.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/summary")
    public ResponseEntity<ApiDataResponse<InvoiceSummaryResponse>> getInvoiceSummary() {

        InvoiceSummaryResponse data = invoiceService.getInvoiceSummary();

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Invoice summary fetched successfully", data));
    }
}