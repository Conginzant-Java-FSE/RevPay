package com.revpay.service;

import com.revpay.dto.AnalyticsSummaryResponse;
import com.revpay.dto.PaymentTrendResponse;
import com.revpay.dto.RevenueDataResponse;
import com.revpay.dto.TopCustomerResponse;
import com.revpay.enums.AccountType;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final TransactionRepository transactionRepository;

    public AnalyticsService(UserRepository userRepository, TransactionRepository transactionRepository) {
        super(userRepository);
        this.transactionRepository = transactionRepository;
    }

    // ── Helpers — resolve date range with sensible defaults
    private LocalDateTime resolveFrom(LocalDate from) {
        return (from != null ? from : LocalDate.now().minusMonths(3))
                .atStartOfDay();
    }

    private LocalDateTime resolveTo(LocalDate to) {
        return (to != null ? to : LocalDate.now()).atTime(23, 59, 59);
    }

    private void assertBusinessAccount(User user) {
        if (user.getAccountType() != AccountType.BUSINESS) {
            throw new IllegalStateException("Analytics are only available for business accounts");
        }
    }

    // ── 1. Summary
    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        LocalDateTime fromDt = resolveFrom(from);
        LocalDateTime toDt   = resolveTo(to);

        BigDecimal totalReceived = transactionRepository.sumReceivedByUserAndDateRange(user, fromDt, toDt);

        BigDecimal totalSent = transactionRepository.sumSentByUserAndDateRange(user, fromDt, toDt);

        BigDecimal pendingIncoming = transactionRepository.sumPendingReceivedByUser(user, fromDt, toDt);

        BigDecimal pendingOutgoing = transactionRepository.sumPendingSentByUser(user, fromDt, toDt);

        BigDecimal netFlow = totalReceived.subtract(totalSent);

        logger.info("Analytics summary fetched for user: {}", user.getEmail());

        return AnalyticsSummaryResponse.builder().totalReceived(totalReceived).totalSent(totalSent)
                .pendingIncoming(pendingIncoming).pendingOutgoing(pendingOutgoing).netFlow(netFlow).build();
    }

    // ── 2. Revenue
    public List<RevenueDataResponse> getRevenue(String period, LocalDate from, LocalDate to) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        LocalDateTime fromDt = resolveFrom(from);
        LocalDateTime toDt   = resolveTo(to);

        List<Transaction> transactions = transactionRepository
                .findCompletedReceivedInRange(user, fromDt, toDt);

        DateTimeFormatter formatter = switch (period.toUpperCase()) {
            case "DAILY"   -> DateTimeFormatter.ofPattern("yyyy-MM-dd");
            case "WEEKLY"  -> DateTimeFormatter.ofPattern("yyyy-'W'ww");
            case "MONTHLY" -> DateTimeFormatter.ofPattern("yyyy-MM");
            default -> throw new IllegalArgumentException("Invalid period. Must be DAILY, WEEKLY, or MONTHLY");
        };

        Map<String, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().format(formatter)
                ));

        List<RevenueDataResponse> result = grouped.entrySet().stream()
                .map(entry -> RevenueDataResponse.builder()
                        .period(entry.getKey()).revenue(entry.getValue().stream().map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .transactionCount((long) entry.getValue().size())
                        .build())
                .sorted((a, b) -> a.getPeriod().compareTo(b.getPeriod()))
                .collect(Collectors.toList());

        return result;
    }

    // ── 3. Top Customers
    public List<TopCustomerResponse> getTopCustomers(int limit, LocalDate from, LocalDate to) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        LocalDateTime fromDt = resolveFrom(from);
        LocalDateTime toDt   = resolveTo(to);

        List<Transaction> transactions = transactionRepository
                .findCompletedReceivedInRange(user, fromDt, toDt);

        Map<User, List<Transaction>> bySender = transactions.stream()
                .filter(t -> t.getSender() != null)
                .collect(Collectors.groupingBy(Transaction::getSender));

        List<TopCustomerResponse> ranked = new ArrayList<>();
        int[] rankCounter = {1};

        bySender.entrySet().stream()
                .map(entry -> {
                    BigDecimal volume = entry.getValue().stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.entry(entry.getKey(), Map.entry(volume, (long) entry.getValue().size()));
                })
                .sorted((a, b) -> b.getValue().getKey().compareTo(a.getValue().getKey())) // sort by volume desc
                .limit(limit)
                .forEach(entry -> {
                    User sender = entry.getKey();
                    BigDecimal volume = entry.getValue().getKey();
                    Long count = entry.getValue().getValue();

                    ranked.add(TopCustomerResponse.builder()
                            .rank(rankCounter[0]++)
                            .customer(TopCustomerResponse.CustomerInfo.builder()
                                    .name(sender.getFullName())
                                    .email(sender.getEmail())
                                    .build())
                            .totalVolume(volume)
                            .transactionCount(count)
                            .build());
                });

        return ranked;
    }

    // ── 4. Payment Trends
    public List<PaymentTrendResponse> getPaymentTrends(LocalDate from, LocalDate to) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        LocalDateTime fromDt = resolveFrom(from);
        LocalDateTime toDt   = resolveTo(to);

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Transaction> incoming = transactionRepository
                .findCompletedReceivedInRange(user, fromDt, toDt);

        List<Transaction> outgoing = transactionRepository
                .findCompletedSentInRange(user, fromDt, toDt);

        Map<String, BigDecimal> incomingByDay = incoming.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().format(dayFormatter),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> outgoingByDay = outgoing.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().format(dayFormatter),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        List<String> allDates = new ArrayList<>();
        allDates.addAll(incomingByDay.keySet());
        outgoingByDay.keySet().forEach(d -> { if (!allDates.contains(d)) allDates.add(d); });
        allDates.sort(String::compareTo);

        return allDates.stream()
                .map(date -> PaymentTrendResponse.builder()
                        .date(date).incoming(incomingByDay.getOrDefault(date, BigDecimal.ZERO))
                        .outgoing(outgoingByDay.getOrDefault(date, BigDecimal.ZERO)).build())
                .collect(Collectors.toList());
    }
}