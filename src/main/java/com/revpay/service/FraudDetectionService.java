package com.revpay.service;

import com.revpay.enums.UserStatus;
import com.revpay.model.FraudLog;
import com.revpay.model.User;
import com.revpay.repository.FraudLogRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudLogRepository fraudLogRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FraudLogService fraudLogService;

    public boolean checkFraud(Long userId, Double amount) {

        // 🔹 Fetch user once
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔹 Safe daily limit handling
        Double dailyLimit = user.getDailyLimit() != null
                ? user.getDailyLimit()
                : 100000.0;

        // 🔹 Auto suspend after 5 blocked fraud attempts
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        long fraudCount = fraudLogRepository
                .countByUser_IdAndBlockedTrueAndDetectedAtAfter(userId, oneDayAgo);
        if (fraudCount >= 7) {
            user.setStatus(UserStatus.SUSPENDED);
            userRepository.save(user);
            return true;
        }

        // 🚨 Rule 1: High amount check
        if (amount > 50000) {
            fraudLogService.saveFraud(user, amount, "High transaction amount", true);
            return true;
        }

        // 🚨 Rule 2: Multiple transactions in 2 minutes
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);

        List<FraudLog> recentLogs =
                fraudLogRepository.findByUser_IdAndDetectedAtAfter(userId, twoMinutesAgo);

        if (recentLogs.size() >= 3) {
            fraudLogService.saveFraud(user, amount, "Too many transactions in short time", true);
            return true;
        }

        // 🚨 Rule 3: Daily limit exceeded
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Double todayTotal = transactionRepository.getTodayTotal(userId, startOfDay);
        if (todayTotal == null) {
            todayTotal = 0.0;
        }

        if (todayTotal + amount > dailyLimit) {
            fraudLogService.saveFraud(user, amount, "Daily transaction limit exceeded", true);
            return true;
        }

        return false;
    }

}