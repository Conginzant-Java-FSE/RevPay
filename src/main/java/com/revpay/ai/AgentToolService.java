package com.revpay.ai;

import com.revpay.dto.TransactionResponseDTO;
import com.revpay.dto.WalletBalanceResponse;
import com.revpay.enums.LoanStatus;
import com.revpay.model.User;
import com.revpay.repository.LoanRepository;
import com.revpay.service.BaseService;
import com.revpay.service.InvoiceService;
import com.revpay.service.TransactionService;
import com.revpay.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes live backend data as "tools" for the AI assistant.
 *
 * Each method is best-effort: exceptions are caught and turned into
 * friendly fallback strings so one tool failure never crashes the chat.
 *
 * Extends BaseService to inherit getLoggedInUser() (same pattern as
 * LoanService and InvoiceService).
 */
@Service
public class AgentToolService extends BaseService {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InvoiceService invoiceService;

    // ── Tool 1: Wallet Balance ────────────────────────────────────────────────

    public String getWalletBalance() {
        try {
            WalletBalanceResponse bal = walletService.getWalletBalance();
            return String.format("Wallet balance: %s %s", bal.getBalance(), bal.getCurrency());
        } catch (Exception e) {
            return "Wallet balance: unavailable";
        }
    }

    // ── Tool 2: Recent Transactions (Spending Insights & Explanations) ────────
    public String getRecentTransactions() {
        try {
            // Fetch more transactions (up to 15) to allow the AI to give spending insights
            List<TransactionResponseDTO> txns = transactionService.getRecentTransactions(15);
            if (txns == null || txns.isEmpty()) {
                return "Recent transactions: none found";
            }
            String lines = txns.stream()
                    .map(t -> {
                        String date = t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate().toString() : "N/A";
                        String type = t.getType() != null ? t.getType() : "UNKNOWN";
                        String amount = t.getAmount() != null ? t.getAmount().toString() : "0.00";
                        String status = t.getStatus() != null ? t.getStatus() : "UNKNOWN";
                        String counterparty = (t.getCounterparty() != null && t.getCounterparty().getFullName() != null)
                                ? " (with " + t.getCounterparty().getFullName() + ")" : "";
                        String balance = t.getBalanceAfter() != null ? " | Bal: ₹" + t.getBalanceAfter() : "";
                        String note = (t.getNote() != null && !t.getNote().isBlank()) ? " | Note: '" + t.getNote() + "'" : "";
                        
                        return String.format("  • [%s] %s ₹%s [%s]%s%s%s", date, type, amount, status, counterparty, balance, note);
                    })
                    .collect(Collectors.joining("\n"));
            return "Recent transactions (up to 15):\n" + lines;
        } catch (Exception e) {
            return "Recent transactions: unavailable";
        }
    }

    // ── Tool 3: Loan Balance ──────────────────────────────────────────────────

    public String getLoanBalance() {
        try {
            User user = getLoggedInUser();
            Pageable one = PageRequest.of(0, 1);
            var page = loanRepository.findByUserAndStatusOrderByCreatedAtDesc(
                    user, LoanStatus.ACTIVE, one);
            if (page.isEmpty()) {
                return "Active loan: none";
            }
            var loan = page.getContent().get(0);
            return String.format("Active loan #%d — outstanding: ₹%s, next due: %s",
                    loan.getLoanId(),
                    loan.getOutstandingBalance(),
                    loan.getNextDueDate() != null ? loan.getNextDueDate() : "N/A");
        } catch (Exception e) {
            return "Loan balance: unavailable";
        }
    }

    // ── Tool 4: Invoice Summary ───────────────────────────────────────────────

    public String getInvoiceSummary() {
        try {
            var s = invoiceService.getInvoiceSummary();
            return String.format(
                    "Invoice summary — Paid: ₹%s (%d), Unpaid: ₹%s (%d), Overdue: ₹%s (%d)",
                    s.getTotalPaid(),    s.getInvoiceCount().getPaid(),
                    s.getTotalUnpaid(),  s.getInvoiceCount().getUnpaid(),
                    s.getTotalOverdue(), s.getInvoiceCount().getOverdue());
        } catch (IllegalStateException e) {
            // Personal accounts: invoices not available — return friendly message
            return "Invoice summary: not available for personal accounts";
        } catch (Exception e) {
            return "Invoice summary: unavailable";
        }
    }
}
