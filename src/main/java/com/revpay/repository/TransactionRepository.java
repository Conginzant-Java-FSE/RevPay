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

    // All transactions for a user (sent or received) — for transaction history
    @Query("SELECT t FROM Transaction t WHERE t.sender = :user OR t.receiver = :user ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUser(@Param("user") User user, Pageable pageable);

    // Filter by type
    @Query("SELECT t FROM Transaction t WHERE (t.sender = :user OR t.receiver = :user) AND t.transactionType = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserAndType(@Param("user") User user, @Param("type") TransactionType type, Pageable pageable);

    // Filter by status
    @Query("SELECT t FROM Transaction t WHERE (t.sender = :user OR t.receiver = :user) AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserAndStatus(@Param("user") User user, @Param("status") TransactionStatus status, Pageable pageable);

    // Filter by date range
    @Query("SELECT t FROM Transaction t WHERE (t.sender = :user OR t.receiver = :user) AND t.createdAt BETWEEN :from AND :to ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUserAndDateRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    // Search by counterparty name, email or transaction ID
    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sender = :user OR t.receiver = :user)
        AND (
            LOWER(t.sender.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.receiver.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.sender.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.receiver.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            CAST(t.transactionId AS string) LIKE CONCAT('%', :search, '%')
        )
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findAllByUserAndSearch(@Param("user") User user, @Param("search") String search, Pageable pageable);

    // For analytics — total received in date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiver = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumReceivedByUserAndDateRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // For analytics — total sent in date range
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sender = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumSentByUserAndDateRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    Optional<Transaction> findByTransactionId(Long transactionId);

    // Completed transactions received by user in date range
    @Query("SELECT t FROM Transaction t WHERE t.receiver = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    List<Transaction> findCompletedReceivedInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Completed transactions sent by user in date range
    @Query("SELECT t FROM Transaction t WHERE t.sender = :user AND t.status = 'COMPLETED' AND t.createdAt BETWEEN :from AND :to")
    List<Transaction> findCompletedSentInRange(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Pending incoming — transactions where this user is receiver but status is PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.receiver = :user AND t.status = 'PENDING' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumPendingReceivedByUser(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Pending outgoing — transactions where this user is sender but status is PENDING
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.sender = :user AND t.status = 'PENDING' AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumPendingSentByUser(@Param("user") User user, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}