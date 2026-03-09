package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.enums.*;
import com.revpay.model.*;
import com.revpay.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    // Auto-approve threshold
    private static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("50000.00");

    // Fixed annual interest rate for simulation
    private static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("8.5");

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // List loans
    public Page<LoanListResponse> getLoans(LoanStatus status, Pageable pageable) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Page<Loan> loans = (status != null)
                ? loanRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable)
                : loanRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return loans.map(loan -> LoanListResponse.builder()
                .loanId(loan.getLoanId())
                .loanAmount(loan.getLoanAmount())
                .purpose(loan.getPurpose())
                .status(loan.getStatus())
                .tenureMonths(loan.getTenureMonths())
                .interestRate(loan.getInterestRate())
                .submittedAt(loan.getCreatedAt())
                .build());
    }

    // Apply for loan
    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        if (loanRepository.existsByUserAndStatus(user, LoanStatus.ACTIVE)) {
            throw new IllegalStateException(
                    "You already have an active loan. Please repay it before applying for a new one.");
        }

        if (loanRepository.existsByUserAndStatus(user, LoanStatus.PENDING)) {
            throw new IllegalStateException(
                    "You already have a pending loan application.");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanAmount(request.getLoanAmount());
        loan.setPurpose(request.getPurpose());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setAnnualRevenue(request.getAnnualRevenue());
        loan.setYearsInBusiness(request.getYearsInBusiness());
        loan.setEmployeeCount(request.getEmployeeCount());
        loan.setCollateral(request.getCollateral());
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.PENDING);

        if (request.getLoanAmount().compareTo(AUTO_APPROVE_LIMIT) < 0) {
            approveLoan(loan);
            logger.info("Loan auto-approved for user: {} amount: {}",
                    user.getEmail(), request.getLoanAmount());
        } else {
            logger.info("Loan application pending manual review for user: {}",
                    user.getEmail());
        }

        loanRepository.save(loan);

        notificationService.sendNotification(
                user,
                NotificationType.GENERAL,
                loan.getStatus() == LoanStatus.APPROVED
                        ? "Your loan of ₹" + request.getLoanAmount() + " has been approved!"
                        : "Your loan application of ₹" + request.getLoanAmount() + " is under review."
        );

        return LoanApplicationResponse.builder()
                .loanId(loan.getLoanId())
                .status(loan.getStatus())
                .loanAmount(loan.getLoanAmount())
                .submittedAt(loan.getCreatedAt())
                .build();
    }

    // Loan detail
    public LoanDetailResponse getLoanDetail(Long loanId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Loan loan = loanRepository.findByLoanIdAndUser(loanId, user)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        return LoanDetailResponse.builder()
                .loanId(loan.getLoanId())
                .status(loan.getStatus())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .tenureMonths(loan.getTenureMonths())
                .monthlyEmi(loan.getEmiAmount())
                .totalInterest(loan.getTotalInterest())
                .totalRepayable(loan.getTotalRepayable())
                .amountRepaid(loan.getAmountRepaid())
                .outstandingBalance(loan.getOutstandingBalance())
                .nextDueDate(loan.getNextDueDate())
                .approvedAt(loan.getApprovedAt())
                .rejectedAt(loan.getRejectedAt())
                .rejectionReason(loan.getRejectionReason())
                .build();
    }

    // Repayment schedule
    public List<RepaymentScheduleResponse> getRepaymentSchedule(Long loanId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Loan loan = loanRepository.findByLoanIdAndUser(loanId, user)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getStatus() == LoanStatus.PENDING
                || loan.getStatus() == LoanStatus.REJECTED) {
            throw new IllegalStateException(
                    "Repayment schedule is only available for approved or active loans");
        }

        List<RepaymentScheduleResponse> schedule = new ArrayList<>();
        LocalDate today = LocalDate.now();

        BigDecimal monthlyRate = ANNUAL_INTEREST_RATE
                .divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);

        BigDecimal remainingPrincipal = loan.getLoanAmount();
        LocalDate dueDate = loan.getApprovedAt() != null
                ? loan.getApprovedAt().toLocalDate().plusMonths(1)
                : today.plusMonths(1);

        int paidInstalments = 0;
        if (loan.getEmiAmount() != null && loan.getEmiAmount().compareTo(BigDecimal.ZERO) > 0
                && loan.getAmountRepaid() != null) {
            paidInstalments = loan.getAmountRepaid()
                    .divide(loan.getEmiAmount(), 0, RoundingMode.FLOOR).intValue();
        }

        for (int i = 1; i <= loan.getTenureMonths(); i++) {

            BigDecimal interestComponent = remainingPrincipal.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalComponent = loan.getEmiAmount().subtract(interestComponent)
                    .setScale(2, RoundingMode.HALF_UP);

            RepaymentScheduleResponse.InstalmentStatus status;
            if (i <= paidInstalments) {
                status = RepaymentScheduleResponse.InstalmentStatus.PAID;
            } else if (dueDate.isBefore(today)) {
                status = RepaymentScheduleResponse.InstalmentStatus.OVERDUE;
            } else {
                status = RepaymentScheduleResponse.InstalmentStatus.UPCOMING;
            }

            schedule.add(RepaymentScheduleResponse.builder()
                    .instalmentNumber(i)
                    .dueDate(dueDate)
                    .emiAmount(loan.getEmiAmount())
                    .principal(principalComponent)
                    .interest(interestComponent)
                    .status(status)
                    .build());

            remainingPrincipal = remainingPrincipal.subtract(principalComponent);
            dueDate = dueDate.plusMonths(1);
        }

        return schedule;
    }

    // Repay loan
    @Transactional
    public LoanRepaymentResponse repayLoan(Long loanId, LoanRepaymentRequest request) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        if (user.getMtPin() == null) {
            throw new IllegalStateException("Transaction PIN not set. Please set your PIN first.");
        }

        if (!passwordEncoder.matches(request.getPin(), user.getMtPin())) {
            throw new IllegalArgumentException("Incorrect transaction PIN");
        }

        Loan loan = loanRepository.findByLoanIdAndUser(loanId, user)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Repayments can only be made on active loans. Current status: "
                            + loan.getStatus());
        }
// HERE IS THE BUG FOR OUTSANDING AMOUNT AND EMI
//        if (request.getAmount().compareTo(loan.getEmiAmount()) < 0) {
//            throw new IllegalArgumentException(
//                    "Repayment amount must be at least the monthly EMI of ₹"
//                            + loan.getEmiAmount());
//        }

//        //CORRECT REPAY AMOUNT BUG FIXED
//        // Minimum EMI check
//        if (request.getAmount().compareTo(loan.getEmiAmount()) < 0) {
//            throw new IllegalArgumentException(
//                    "Repayment amount must be at least the monthly EMI of ₹" + loan.getEmiAmount());
//        }
//
//Cannot exceed remaining loan
//        if (request.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
//            throw new IllegalArgumentException(
//                    "Repayment amount cannot exceed outstanding balance of ₹" + loan.getOutstandingBalance());
//        }


        BigDecimal minPayment = loan.getOutstandingBalance().min(loan.getEmiAmount());

        if (request.getAmount().compareTo(minPayment) < 0) {
            throw new IllegalArgumentException(
                    "Repayment amount must be at least ₹" + minPayment
            );
        }

        if (request.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Repayment amount cannot exceed outstanding balance of ₹" + loan.getOutstandingBalance()
            );
        }
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException(
                    "Insufficient wallet balance. Available: ₹" + wallet.getBalance());
        }

        BigDecimal newWalletBalance = wallet.getBalance().subtract(request.getAmount());
        wallet.setBalance(newWalletBalance);
        walletRepository.save(wallet);

        BigDecimal newAmountRepaid = loan.getAmountRepaid().add(request.getAmount());
        BigDecimal newOutstanding  = loan.getOutstandingBalance().subtract(request.getAmount());

        if (newOutstanding.compareTo(BigDecimal.ZERO) < 0) {
            newOutstanding = BigDecimal.ZERO;
        }

        loan.setAmountRepaid(newAmountRepaid);
        loan.setOutstandingBalance(newOutstanding);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setNextDueDate(null);
            logger.info("Loan {} fully repaid and closed for user: {}", loanId, user.getEmail());
        } else {
            loan.setNextDueDate(loan.getNextDueDate().plusMonths(1));
        }

        loanRepository.save(loan);

        Transaction transaction = new Transaction();
        transaction.setSender(user);
        transaction.setReceiver(null);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(TransactionType.LOAN_REPAYMENT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBalanceAfter(newWalletBalance);
        transaction.setNote("Loan repayment for loan #" + loanId);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        String message = newOutstanding.compareTo(BigDecimal.ZERO) == 0
                ? "Congratulations! Your loan #" + loanId + " has been fully repaid."
                : "Loan repayment of ₹" + request.getAmount()
                + " received. Outstanding balance: ₹" + newOutstanding;

        notificationService.sendNotification(
                user,
                NotificationType.GENERAL,
                message
        );

        logger.info("Loan repayment of {} processed for user: {}", request.getAmount(), user.getEmail());

        return LoanRepaymentResponse.builder()
                .newBalance(newWalletBalance)
                .amountRepaid(newAmountRepaid)
                .outstandingBalance(newOutstanding)
                .build();
    }

    // EMI calculation using standard reducing balance formula
//    private void approveLoan(Loan loan) {
//
//        BigDecimal principal    = loan.getLoanAmount();
//        int        tenure       = loan.getTenureMonths();
//        BigDecimal annualRate   = ANNUAL_INTEREST_RATE;
//
//        BigDecimal monthlyRate  = annualRate.divide(
//                BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
//
//        BigDecimal onePlusR     = BigDecimal.ONE.add(monthlyRate);
//        BigDecimal onePlusRPowN = onePlusR.pow(tenure, new MathContext(10));
//
//        BigDecimal emi = principal
//                .multiply(monthlyRate)
//                .multiply(onePlusRPowN)
//                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
//
//        BigDecimal totalRepayable = emi.multiply(BigDecimal.valueOf(tenure))
//                .setScale(2, RoundingMode.HALF_UP);
//        BigDecimal totalInterest  = totalRepayable.subtract(principal)
//                .setScale(2, RoundingMode.HALF_UP);
//
//        loan.setStatus(LoanStatus.ACTIVE);
//        loan.setInterestRate(annualRate);
//        loan.setEmiAmount(emi);
//        loan.setTotalInterest(totalInterest);
//        loan.setTotalRepayable(totalRepayable);
//        loan.setOutstandingBalance(totalRepayable);
//        loan.setAmountRepaid(BigDecimal.ZERO);
//        loan.setApprovedAt(LocalDateTime.now());
//        loan.setNextDueDate(LocalDate.now().plusMonths(1));
//    }
    private void approveLoan(Loan loan) {

        BigDecimal principal = loan.getLoanAmount();
        int tenure = loan.getTenureMonths();
        BigDecimal annualRate = ANNUAL_INTEREST_RATE;

        BigDecimal monthlyRate = annualRate.divide(
                BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowN = onePlusR.pow(tenure, new MathContext(10));

        BigDecimal emi = principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        BigDecimal totalRepayable = emi.multiply(BigDecimal.valueOf(tenure))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterest = totalRepayable.subtract(principal)
                .setScale(2, RoundingMode.HALF_UP);

        // Loan details
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setInterestRate(annualRate);
        loan.setEmiAmount(emi);
        loan.setTotalInterest(totalInterest);
        loan.setTotalRepayable(totalRepayable);
        loan.setOutstandingBalance(totalRepayable);
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setApprovedAt(LocalDateTime.now());
        loan.setNextDueDate(LocalDate.now().plusMonths(1));

        // 🔹 CREDIT LOAN AMOUNT TO WALLET
        Wallet wallet = walletRepository.findByUser(loan.getUser())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        BigDecimal newBalance = wallet.getBalance().add(principal);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // 🔹 Create transaction record
        Transaction transaction = new Transaction();
        transaction.setSender(null);
        transaction.setReceiver(loan.getUser());
        transaction.setAmount(principal);
        transaction.setTransactionType(TransactionType.LOAN_DIS);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setBalanceAfter(newBalance);
        transaction.setNote("Loan disbursed for loan #" + loan.getLoanId());
        transaction.setCreatedAt(LocalDateTime.now());

        transactionRepository.save(transaction);
    }
    //Guard — business accounts only
    private void assertBusinessAccount(User user) {
        if (user.getAccountType() != AccountType.BUSINESS) {
            throw new IllegalStateException(
                    "This feature is only available for business accounts");
        }
    }
}