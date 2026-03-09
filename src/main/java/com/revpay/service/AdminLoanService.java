package com.revpay.service;

import com.revpay.enums.LoanStatus;
import com.revpay.enums.NotificationType;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.Loan;
import com.revpay.model.Transaction;
import com.revpay.model.Wallet;
import com.revpay.repository.LoanRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminLoanService {

    private static final Logger logger = LoggerFactory.getLogger(AdminLoanService.class);

    // Same rate as LoanService — change if bank-specific rates needed later
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("8.5");

    private final LoanRepository loanRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    // ── GET ALL LOANS (admin sees every loan, optionally filtered by status) ──
    public Page<Map<String, Object>> getAllLoans(LoanStatus status, Pageable pageable) {

        Page<Loan> loans = (status != null)
                ? loanRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : loanRepository.findAllByOrderByCreatedAtDesc(pageable);

        return loans.map(this::toLoanMap);
    }

    // ── GET PENDING LOANS ONLY ─────────────────────────────────────────────────
    public Page<Map<String, Object>> getPendingLoans(Pageable pageable) {
        return loanRepository
                .findByStatusOrderByCreatedAtDesc(LoanStatus.PENDING, pageable)
                .map(this::toLoanMap);
    }

    // ── GET SINGLE LOAN DETAIL ─────────────────────────────────────────────────
    public Map<String, Object> getLoanDetail(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        return toLoanDetailMap(loan);
    }

    // ── APPROVE LOAN ───────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> approveLoan(Long loanId, String adminNote) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING loans can be approved. Current status: " + loan.getStatus());
        }

        // ── EMI Calculation (same formula as LoanService.approveLoan) ──────────
        BigDecimal principal   = loan.getLoanAmount();
        int        tenure      = loan.getTenureMonths();
        BigDecimal annualRate  = ANNUAL_INTEREST_RATE;

        BigDecimal monthlyRate   = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR      = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN  = onePlusR.pow(tenure, new MathContext(10));

        BigDecimal emi = principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        BigDecimal totalRepayable = emi.multiply(BigDecimal.valueOf(tenure))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInterest  = totalRepayable.subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);

        // ── Update loan fields ─────────────────────────────────────────────────
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setInterestRate(annualRate);
        loan.setEmiAmount(emi);
        loan.setTotalInterest(totalInterest);
        loan.setTotalRepayable(totalRepayable);
        loan.setOutstandingBalance(totalRepayable);
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setNextDueDate(LocalDate.now().plusMonths(1));
        loan.setUpdatedBy("ADMIN");

        loanRepository.save(loan);

        // ── Credit loan amount to user wallet ──────────────────────────────────
        Wallet wallet = walletRepository.findByUser(loan.getUser())
                .orElseThrow(() -> new IllegalStateException("User wallet not found"));

        BigDecimal newBalance = wallet.getBalance().add(principal);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // ── Create disbursement transaction record ─────────────────────────────
        Transaction tx = new Transaction();
        tx.setSender(null);
        tx.setReceiver(loan.getUser());
        tx.setAmount(principal);
        tx.setTransactionType(TransactionType.LOAN_DIS);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setBalanceAfter(newBalance);
        tx.setNote("Loan #" + loanId + " approved and disbursed by admin");
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // ── Notify user ────────────────────────────────────────────────────────
        notificationService.sendNotification(
                loan.getUser(),
                NotificationType.GENERAL,
                "Your loan of ₹" + principal + " has been approved! EMI: ₹" + emi + "/month."
        );

        logger.info("Admin approved loan #{} for user: {} amount: {}",
                loanId, loan.getUser().getEmail(), principal);

        return toLoanDetailMap(loan);
    }

    // ── REJECT LOAN ────────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> rejectLoan(Long loanId, String reason) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING loans can be rejected. Current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectedAt(LocalDateTime.now());
        loan.setRejectionReason(reason);
        loan.setUpdatedBy("ADMIN");

        loanRepository.save(loan);

        // ── Notify user ────────────────────────────────────────────────────────
        notificationService.sendNotification(
                loan.getUser(),
                NotificationType.GENERAL,
                "Your loan application of ₹" + loan.getLoanAmount()
                        + " has been rejected. Reason: " + reason
        );

        logger.info("Admin rejected loan #{} for user: {} reason: {}",
                loanId, loan.getUser().getEmail(), reason);

        return toLoanDetailMap(loan);
    }

    // ── MAPPER: list view (lightweight) ───────────────────────────────────────
    private Map<String, Object> toLoanMap(Loan loan) {
        Map<String, Object> map = new HashMap<>();
        map.put("loanId",          loan.getLoanId());
        map.put("applicantName",   loan.getUser().getFullName());
        map.put("applicantEmail",  loan.getUser().getEmail());
        map.put("loanAmount",      loan.getLoanAmount());
        map.put("purpose",         loan.getPurpose());
        map.put("tenureMonths",    loan.getTenureMonths());
        map.put("status",          loan.getStatus());
        map.put("submittedAt",     loan.getCreatedAt());
        map.put("interestRate",    loan.getInterestRate());
        map.put("emiAmount",       loan.getEmiAmount());
        return map;
    }

    // ── MAPPER: detail view (full) ─────────────────────────────────────────────
    private Map<String, Object> toLoanDetailMap(Loan loan) {
        Map<String, Object> map = new HashMap<>();
        map.put("loanId",            loan.getLoanId());
        map.put("applicantName",     loan.getUser().getFullName());
        map.put("applicantEmail",    loan.getUser().getEmail());
        map.put("applicantPhone",    loan.getUser().getPhone());
        map.put("loanAmount",        loan.getLoanAmount());
        map.put("purpose",           loan.getPurpose());
        map.put("tenureMonths",      loan.getTenureMonths());
        map.put("annualRevenue",     loan.getAnnualRevenue());
        map.put("yearsInBusiness",   loan.getYearsInBusiness());
        map.put("employeeCount",     loan.getEmployeeCount());
        map.put("collateral",        loan.getCollateral());
        map.put("status",            loan.getStatus());
        map.put("interestRate",      loan.getInterestRate());
        map.put("emiAmount",         loan.getEmiAmount());
        map.put("totalInterest",     loan.getTotalInterest());
        map.put("totalRepayable",    loan.getTotalRepayable());
        map.put("outstandingBalance",loan.getOutstandingBalance());
        map.put("amountRepaid",      loan.getAmountRepaid());
        map.put("nextDueDate",       loan.getNextDueDate());
        map.put("rejectionReason",   loan.getRejectionReason());
        map.put("submittedAt",       loan.getCreatedAt());
        map.put("approvedAt",        loan.getApprovedAt());
        map.put("rejectedAt",        loan.getRejectedAt());
        return map;
    }
}