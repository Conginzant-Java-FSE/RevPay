package com.revpay.service;


import com.revpay.model.FraudLog;
import com.revpay.model.User;
import com.revpay.repository.FraudLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraudLogService {

    private final FraudLogRepository fraudLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFraud(User user, Double amount, String reason, boolean blocked) {

        FraudLog log = new FraudLog();
        log.setUser(user);
        log.setAmount(amount);
        log.setReason(reason);
        log.setBlocked(blocked);
        log.setDetectedAt(LocalDateTime.now());

        fraudLogRepository.save(log);
    }
}
