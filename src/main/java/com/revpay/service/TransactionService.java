package com.revpay.service;


import com.revpay.dto.SendMoneyRequest;
import com.revpay.dto.TransactionResponseDTO;
import com.revpay.enums.NotificationType;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.model.Wallet;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

        private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
        private static final int MAX_PAGE_SIZE = 100;
        private static final int RECENT_LIMIT = 5;

        @Autowired
        private TransactionRepository transactionRepository;
        @Autowired
        private UserRepository userRepository;
        @Autowired
        private WalletRepository walletRepository;
        @Autowired
        private BCryptPasswordEncoder passwordEncoder;
        @Autowired
        private NotificationService notificationService;

        @Autowired
        private FraudDetectionService fraudDetectionService;

        // ─────────────────────────────────────────────────────────────────────────
        // HELPER — resolve currently logged-in user
        // ─────────────────────────────────────────────────────────────────────────
        private User getLoggedInUser() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || auth.getName() == null) {
                        throw new RuntimeException("User not authenticated");
                }
                return userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("User not found"));
        }

        // =========================================================================
        // FEATURE 1 — GET TRANSACTION HISTORY (paginated + combined filters)
        // GET /api/transactions
        // =========================================================================
        public Map<String, Object> getTransactions(
                int page,
                int size,
                TransactionType type,
                TransactionStatus status,
                LocalDate from,
                LocalDate to,
                String search) {

                User user = getLoggedInUser();

                size = (size <= 0) ? 10 : Math.min(size, MAX_PAGE_SIZE);
                page = Math.max(page, 0);
                Pageable pageable = PageRequest.of(page, size);

                LocalDateTime fromDT = (from != null) ? from.atStartOfDay() : null;
                LocalDateTime toDT = (to != null) ? to.atTime(LocalTime.MAX) : null;

                String keyword = (search != null && !search.isBlank()) ? search.trim() : null;

                Page<Transaction> resultPage = transactionRepository.findAllByUserWithFilters(
                        user, type, status, fromDT, toDT, keyword, pageable);

                List<TransactionResponseDTO> data = resultPage.getContent().stream()
                        .map(t -> toDTO(t, user))
                        .collect(Collectors.toList());

                Map<String, Object> pagination = new LinkedHashMap<>();
                pagination.put("currentPage", resultPage.getNumber());
                pagination.put("pageSize", resultPage.getSize());
                pagination.put("totalRecords", resultPage.getTotalElements());
                pagination.put("totalPages", resultPage.getTotalPages());
                pagination.put("hasNext", resultPage.hasNext());
                pagination.put("hasPrevious", resultPage.hasPrevious());

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("data", data);
                response.put("pagination", pagination);

                logger.info("Transaction history fetched for user: {} | page={} size={} total={}",
                        user.getEmail(), page, size, resultPage.getTotalElements());

                return response;
        }

        // =========================================================================
        // FEATURE 2 — GET SINGLE TRANSACTION DETAIL
        // GET /api/transactions/{transactionId}
        // =========================================================================
        public TransactionResponseDTO getTransactionById(Long transactionId) {

                User user = getLoggedInUser();

                Transaction transaction = transactionRepository.findByIdAndUser(transactionId, user)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Transaction not found with id: " + transactionId));

                return toDTO(transaction, user);
        }

        // =========================================================================
        // FEATURE 3 — SEND MONEY (wallet-to-wallet transfer)
        // POST /api/transactions/send
        // =========================================================================
        @Transactional
        public TransactionResponseDTO sendMoney(SendMoneyRequest request) {

                User sender = getLoggedInUser();

                // 1. Verify transaction PIN
                if (sender.getMtPin() == null) {
                        throw new IllegalStateException("Transaction PIN not set. Please set your PIN first.");
                }
                if (!passwordEncoder.matches(request.getPin(), sender.getMtPin())) {
                        throw new IllegalArgumentException("Incorrect transaction PIN");
                }

                // 2. Look up receiver by email or phone
                User receiver = userRepository.findByEmail(request.getReceiverEmailOrPhone())
                        .or(() -> userRepository.findByPhone(request.getReceiverEmailOrPhone()))
                        .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

                // 3. Cannot send to yourself
                if (sender.getId().equals(receiver.getId())) {
                        throw new IllegalArgumentException("You cannot send money to yourself");
                }

                // 4. Receiver must be active
                if (!receiver.isActive()) {
                        throw new IllegalArgumentException("Receiver account is inactive");
                }

                // 5. Check sender wallet balance
                Wallet senderWallet = walletRepository.findByUser(sender)
                        .orElseThrow(() -> new IllegalStateException("Sender wallet not found"));

                if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                        throw new IllegalArgumentException("Insufficient wallet balance");
                }


                // 🔍 FRAUD CHECK
                validateFraud(sender, request.getAmount());



                // 6. Debit sender
                BigDecimal senderBalanceAfter = senderWallet.getBalance().subtract(request.getAmount());
                senderWallet.setBalance(senderBalanceAfter);
                walletRepository.save(senderWallet);

                // 7. Credit receiver
                Wallet receiverWallet = walletRepository.findByUser(receiver)
                        .orElseThrow(() -> new IllegalStateException("Receiver wallet not found"));

                BigDecimal receiverBalanceAfter = receiverWallet.getBalance().add(request.getAmount());
                receiverWallet.setBalance(receiverBalanceAfter);
                walletRepository.save(receiverWallet);

                // 8. Create transaction record
                Transaction transaction = new Transaction();
                transaction.setSender(sender);
                transaction.setReceiver(receiver);
                transaction.setAmount(request.getAmount());
                transaction.setTransactionType(TransactionType.SEND);
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setBalanceAfter(senderBalanceAfter);
                transaction.setCurrency("INR");
                transaction.setNote(request.getNote());

                transaction.setUtrNumber(
                        "UTR" + System.currentTimeMillis() +
                                UUID.randomUUID().toString().substring(0,6).toUpperCase()
                );
                transactionRepository.save(transaction);

                // 9. Notify both parties
                notificationService.sendNotification(
                        sender,
                        NotificationType.TRANSACTION_SENT,
                        "You sent ₹" + request.getAmount() + " to " + receiver.getFullName());
                notificationService.sendNotification(
                        receiver,
                        NotificationType.TRANSACTION_RECEIVED,
                        "You received ₹" + request.getAmount() + " from " + sender.getFullName());

                // 10. Low balance warning (threshold: ₹100)
                if (senderBalanceAfter.compareTo(new BigDecimal("100")) < 0) {
                        notificationService.sendNotification(
                                sender,
                                NotificationType.LOW_BALANCE,
                                "Your wallet balance is low: ₹" + senderBalanceAfter);
                }

                logger.info("Money transfer: {} → {} | amount={}",
                        sender.getEmail(), receiver.getEmail(), request.getAmount());

                return toDTO(transaction, sender);
        }

        private void validateFraud(User sender, @NotNull(message = "Amount is required") @DecimalMin(value = "1.00", message = "Minimum transfer amount is 1.00") BigDecimal amount) {

                        boolean isFraud = fraudDetectionService
                                .checkFraud(sender.getId(), amount.doubleValue());

                        if (isFraud) {

                                // Save FAILED transaction
                                Transaction failedTransaction = new Transaction();
                                failedTransaction.setSender(sender);
                                failedTransaction.setAmount(amount);
                                failedTransaction.setTransactionType(TransactionType.SEND);
                                failedTransaction.setStatus(TransactionStatus.FAILED);
                                failedTransaction.setCurrency("INR");
                                failedTransaction.setNote("Blocked due to suspected fraud");
                                transactionRepository.save(failedTransaction);

                                // Optional: Notify user
                                notificationService.sendNotification(
                                        sender,
                                        NotificationType.SECURITY_ALERT,
                                        "Transaction blocked due to suspicious activity."
                                );

                                throw new IllegalStateException("Transaction blocked due to suspected fraud.");
                        }

        }

        // =========================================================================
        // FEATURE 4 — RECENT TRANSACTIONS (last 5, for dashboard)
        // GET /api/transactions/recent
        // =========================================================================
        public List<TransactionResponseDTO> getRecentTransactions() {

                User user = getLoggedInUser();

                List<Transaction> recent = transactionRepository
                        .findRecentByUser(user, PageRequest.of(0, RECENT_LIMIT));

                return recent.stream()
                        .map(t -> toDTO(t, user))
                        .collect(Collectors.toList());
        }

        // =========================================================================
        // FEATURE 5 — TRANSACTION SUMMARY / ANALYTICS
        // GET /api/transactions/summary
        // =========================================================================
        public Map<String, Object> getTransactionSummary(LocalDate from, LocalDate to) {

                User user = getLoggedInUser();

                // Default to current month if not provided
                LocalDate startDate = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
                LocalDate endDate = (to != null) ? to : LocalDate.now();

                LocalDateTime fromDT = startDate.atStartOfDay();
                LocalDateTime toDT = endDate.atTime(LocalTime.MAX);

                BigDecimal totalReceived = transactionRepository
                        .sumReceivedByUserAndDateRange(user, fromDT, toDT);
                BigDecimal totalSent = transactionRepository
                        .sumSentByUserAndDateRange(user, fromDT, toDT);
                BigDecimal totalTopUps = transactionRepository
                        .sumTopUpsByUserAndDateRange(user, fromDT, toDT);
                long totalCount = transactionRepository.countAllByUser(user);
                long successCount = transactionRepository.countByUserAndStatus(user, TransactionStatus.SUCCESS);
                long failedCount = transactionRepository.countByUserAndStatus(user, TransactionStatus.FAILED);
                long pendingCount = transactionRepository.countByUserAndStatus(user, TransactionStatus.PENDING);

                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("period", Map.of("from", startDate.toString(), "to", endDate.toString()));
                summary.put("totalReceived", totalReceived);
                summary.put("totalSent", totalSent);
                summary.put("totalTopUps", totalTopUps);
                summary.put("netFlow", totalReceived.subtract(totalSent));
                summary.put("totalCount", totalCount);
                summary.put("successCount", successCount);
                summary.put("failedCount", failedCount);
                summary.put("pendingCount", pendingCount);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("success", true);
                response.put("data", summary);

                return response;
        }

        // =========================================================================
        // HELPER — Entity → DTO mapping
        // =========================================================================
        private TransactionResponseDTO toDTO(Transaction t, User currentUser) {

                // Counterparty = the other person (not the current user)
                // Null for ADD_MONEY / WITHDRAW where sender or receiver may be null
                User counterpartyUser = null;

                if (t.getSender() != null && !t.getSender().getId().equals(currentUser.getId())) {
                        counterpartyUser = t.getSender();
                } else if (t.getReceiver() != null && !t.getReceiver().getId().equals(currentUser.getId())) {
                        counterpartyUser = t.getReceiver();
                }

                TransactionResponseDTO.CounterpartyDTO counterparty = null;
                if (counterpartyUser != null) {
                        final User cp = counterpartyUser;
                        // FIX: removed .phone() — CounterpartyDTO does not have a phone field
                        counterparty = TransactionResponseDTO.CounterpartyDTO.builder()
                                .userId(cp.getId())
                                .fullName(cp.getFullName())
                                .email(cp.getEmail())
                                .build();
                }

                String displayType = t.getTransactionType() != null ? t.getTransactionType().name() : null;

                // If this is a peer-to-peer SEND transfer and the current user is the receiver,
                // display it as RECEIVE so the UI shows it correctly as incoming money (green).
                if ("SEND".equals(displayType) && t.getReceiver() != null
                        && t.getReceiver().getId().equals(currentUser.getId())) {
                        displayType = "RECEIVE";
                }

                return TransactionResponseDTO.builder()
                        .transactionId(t.getTransactionId())
                        .type(displayType)
                        .status(t.getStatus() != null ? t.getStatus().name() : null)
                        .amount(t.getAmount())
                        .currency(t.getCurrency())
                        .note(t.getNote())
                        .counterparty(counterparty)
                        .balanceAfter(t.getBalanceAfter())
                        .createdAt(t.getCreatedAt())
                        .completedAt(t.getUpdatedAt())
                        .build();
        }

        // =========================================================================
        // FEATURE 6 — EXPORT TRANSACTIONS
        // GET /api/transactions/export
        // =========================================================================
        public void exportTransactions(
                String format,
                TransactionType type,
                TransactionStatus status,
                LocalDate from,
                LocalDate to,
                String search,
                HttpServletResponse response) throws IOException {

                User user = getLoggedInUser();

                LocalDateTime fromDT = (from != null) ? from.atStartOfDay() : null;
                LocalDateTime toDT = (to != null) ? to.atTime(LocalTime.MAX) : null;
                String keyword = (search != null && !search.isBlank()) ? search.trim() : null;

                // Fetch all matching records (unpaginated)
                Page<Transaction> resultPage = transactionRepository.findAllByUserWithFilters(
                        user, type, status, fromDT, toDT, keyword, Pageable.unpaged());

                List<TransactionResponseDTO> data = resultPage.getContent().stream()
                        .map(t -> toDTO(t, user))
                        .collect(Collectors.toList());

                if ("CSV".equalsIgnoreCase(format)) {
                        exportToCsv(data, response);
                } else if ("PDF".equalsIgnoreCase(format)) {
                        exportToPdf(data, response);
                } else {
                        throw new IllegalArgumentException("Unsupported format. Use CSV or PDF.");
                }
        }

        private void exportToCsv(List<TransactionResponseDTO> data, HttpServletResponse response) throws IOException {
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=\"revpay-transactions.csv\"");

                CSVFormat csvFormat = CSVFormat.Builder.create()
                        .setHeader("Transaction ID", "Date", "Type", "Status", "Amount", "Currency",
                                "Counterparty", "Balance After")
                        .build();

                try (CSVPrinter printer = new CSVPrinter(response.getWriter(), csvFormat)) {
                        for (TransactionResponseDTO t : data) {
                                String counterpartyName = t.getCounterparty() != null
                                        ? t.getCounterparty().getFullName()
                                        : "N/A";
                                printer.printRecord(
                                        t.getTransactionId(),
                                        t.getCreatedAt().toString(),
                                        t.getType(),
                                        t.getStatus(),
                                        t.getAmount(),
                                        t.getCurrency(),
                                        counterpartyName,
                                        t.getBalanceAfter());
                        }
                }
        }

        private void exportToPdf(List<TransactionResponseDTO> data, HttpServletResponse response) throws IOException {
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=\"revpay-transactions.pdf\"");

                Document document = new Document(PageSize.A4);
                PdfWriter.getInstance(document, response.getOutputStream());

                document.open();

                Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
                fontTitle.setSize(18);
                Paragraph paragraph = new Paragraph("Transaction History", fontTitle);
                paragraph.setAlignment(Paragraph.ALIGN_CENTER);
                document.add(paragraph);

                document.add(new Paragraph(" "));

                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100f);
                table.setWidths(new float[] { 1.5f, 2.5f, 1.5f, 1.5f, 1.5f, 2.5f, 1.5f });

                Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
                fontHeader.setSize(10);

                String[] headers = { "ID", "Date", "Type", "Status", "Amount", "Counterparty", "Balance" };
                for (String header : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
                        table.addCell(cell);
                }

                Font fontRow = FontFactory.getFont(FontFactory.HELVETICA);
                fontRow.setSize(9);

                for (TransactionResponseDTO t : data) {
                        table.addCell(new Phrase(String.valueOf(t.getTransactionId()), fontRow));
                        table.addCell(new Phrase(t.getCreatedAt().toLocalDate().toString(), fontRow));
                        table.addCell(new Phrase(t.getType(), fontRow));
                        table.addCell(new Phrase(t.getStatus(), fontRow));
                        table.addCell(new Phrase(t.getAmount().toString() + " " + t.getCurrency(), fontRow));

                        String cpName = t.getCounterparty() != null ? t.getCounterparty().getFullName() : "N/A";
                        table.addCell(new Phrase(cpName, fontRow));
                        table.addCell(new Phrase(t.getBalanceAfter().toString(), fontRow));
                }

                document.add(table);
                document.close();
        }


}