package com.revpay.repository;


import com.revpay.model.FraudLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {


    List<FraudLog> findByUser_IdAndDetectedAtAfter(Long userId, LocalDateTime time);

    long countByUser_IdAndBlockedTrueAndDetectedAtAfter(Long userId, LocalDateTime time);

}