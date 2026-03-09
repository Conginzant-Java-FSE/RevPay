package com.revpay.controller;

import com.revpay.enums.LoanStatus;
import com.revpay.service.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/loans")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLoansController {

    private final AdminLoanService adminLoanService;

    // GET /api/admin/loans?status=PENDING&page=0&size=20
    @GetMapping
    public ResponseEntity<?> getAllLoans(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        LoanStatus loanStatus = null;
        if (status != null && !status.isBlank()) {
            try { loanStatus = LoanStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<Map<String, Object>> result = adminLoanService.getAllLoans(loanStatus, pageable);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "content",       result.getContent(),
                        "totalElements", result.getTotalElements(),
                        "totalPages",    result.getTotalPages(),
                        "currentPage",   result.getNumber()
                )
        ));
    }

    // GET /api/admin/loans/{loanId}
    @GetMapping("/{loanId}")
    public ResponseEntity<?> getLoanDetail(@PathVariable Long loanId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminLoanService.getLoanDetail(loanId)
        ));
    }

    // PATCH /api/admin/loans/{loanId}/approve  — Angular: approveLoan(loanId)
    @PatchMapping("/{loanId}/approve")
    public ResponseEntity<?> approveLoan(
            @PathVariable Long loanId,
            @RequestBody(required = false) ApproveRequest body) {

        String adminNote = (body != null) ? body.adminNote() : null;
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminLoanService.approveLoan(loanId, adminNote)
        ));
    }

    // PATCH /api/admin/loans/{loanId}/reject  — Angular: rejectLoan(loanId, reason)
    @PatchMapping("/{loanId}/reject")
    public ResponseEntity<?> rejectLoan(
            @PathVariable Long loanId,
            @RequestBody RejectRequest body) {

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", adminLoanService.rejectLoan(loanId, body.reason())
        ));
    }

    record ApproveRequest(String adminNote) {}
    record RejectRequest(String reason) {}
}