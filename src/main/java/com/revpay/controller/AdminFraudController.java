package com.revpay.controller;

import com.revpay.enums.UserStatus;
import com.revpay.model.FraudLog;
import com.revpay.model.User;
import com.revpay.repository.FraudLogRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/fraud-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminFraudController {

    private final FraudLogRepository fraudLogRepository;
    private final UserRepository userRepository;

    // GET /api/admin/fraud-logs
    @GetMapping
    public ResponseEntity<?> getFraudLogs() {
        List<FraudLog> logs = fraudLogRepository.findAllByOrderByDetectedAtDesc();
        List<Map<String, Object>> data = logs.stream().map(this::toMap).toList();
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // PATCH /api/admin/fraud-logs/{fraudLogId}/block
    @PatchMapping("/{fraudLogId}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long fraudLogId) {

        FraudLog log = fraudLogRepository.findById(fraudLogId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud log not found: " + fraudLogId));

        // Mark fraud log blocked
        log.setBlocked(true);
        fraudLogRepository.save(log);

        // Suspend the user
        User user = log.getUser();
        user.setActive(false);
        user.setStatus(UserStatus.SUSPENDED);
        user.setDeactivationReason("Blocked due to fraud: " + log.getReason());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User " + user.getEmail() + " has been blocked"
        ));
    }

    private Map<String, Object> toMap(FraudLog f) {
        return Map.of(
                "id",           f.getId(),
                "userId",       f.getUser().getId(),
                "userFullName", f.getUser().getFullName(),
                "userEmail",    f.getUser().getEmail(),
                "amount",       f.getAmount() != null ? f.getAmount() : 0,
                "reason",       f.getReason(),
                "blocked",      f.isBlocked(),
                "detectedAt",   f.getDetectedAt().toString()
        );
    }
}