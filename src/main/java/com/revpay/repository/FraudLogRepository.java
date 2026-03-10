package com.revpay.repository;


import com.revpay.model.FraudLog;
import com.revpay.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudLogRepository extends JpaRepository<FraudLog, Long> {


    List<FraudLog> findByUser_IdAndDetectedAtAfter(Long userId, LocalDateTime time);

    long countByUser_IdAndBlockedTrueAndDetectedAtAfter(Long userId, LocalDateTime time);
    List<FraudLog> findAllByOrderByDetectedAtDesc();

    // Paginated version for dashboard
    Page<FraudLog> findAllByOrderByDetectedAtDesc(Pageable pageable);

    // Filter by specific user
    Page<FraudLog> findByUserIdOrderByDetectedAtDesc(Long userId, Pageable pageable);

    // Stats counts
    long countByUser(User user);
    long countByBlocked(boolean blocked);
}