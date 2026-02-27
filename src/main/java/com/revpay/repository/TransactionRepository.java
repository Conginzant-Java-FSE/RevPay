package com.revpay.repository;

import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.Transaction;
import com.revpay.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Pending incoming — transactions where this user is receiver but status is PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiver = :user AND t.status = 'PENDING' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumPendingReceivedByUser(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Pending outgoing — transactions where this user is sender but status is PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sender = :user AND t.status = 'PENDING' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumPendingSentByUser(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Completed transactions received by user in date range
    @Query("SELECT t FROM Transaction t WHERE t.receiver = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    List<Transaction> findCompletedReceivedInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Completed transactions sent by user in date range
    @Query("SELECT t FROM Transaction t WHERE t.sender = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    List<Transaction> findCompletedSentInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sender = :user OR t.receiver = :user)
        AND (:type IS NULL OR t.transactionType = :type)
        AND (:status IS NULL OR t.status = :status)
        AND (:from IS NULL OR t.createdAt >= :from)
        AND (:to IS NULL OR t.createdAt <= :to)
        AND (
            :search IS NULL OR
            LOWER(CAST(t.transactionId AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.sender.fullName)  LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.sender.email)     LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.receiver.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.receiver.email)    LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findAllByUserWithFilters(
            @Param("user")   User user,
            @Param("type")   TransactionType type,
            @Param("status") TransactionStatus status,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.transactionId = :id
        AND (t.sender = :user OR t.receiver = :user)
        """)
    Optional<Transaction> findByIdAndUser(
            @Param("id")   Long id,
            @Param("user") User user
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sender = :user OR t.receiver = :user)
        ORDER BY t.createdAt DESC
        """)
    List<Transaction> findRecentByUser(@Param("user") User user, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.receiver = :user
        AND t.transactionType = 'SEND'
        AND t.status = 'SUCCESS'
        AND t.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumReceivedByUserAndDateRange(
            @Param("user") User user,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.sender = :user
        AND t.transactionType = 'SEND'
        AND t.status = 'SUCCESS'
        AND t.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumSentByUserAndDateRange(
            @Param("user") User user,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.receiver = :user
        AND t.transactionType = 'ADD_MONEY'
        AND t.status = 'SUCCESS'
        AND t.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumTopUpsByUserAndDateRange(
            @Param("user") User user,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE (t.sender = :user OR t.receiver = :user)")
    long countAllByUser(@Param("user") User user);

    @Query("""
        SELECT COUNT(t) FROM Transaction t
        WHERE (t.sender = :user OR t.receiver = :user)
        AND t.status = :status
        """)
    long countByUserAndStatus(
            @Param("user")   User user,
            @Param("status") TransactionStatus status
    );

}