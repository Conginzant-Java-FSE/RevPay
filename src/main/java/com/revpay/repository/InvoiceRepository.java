package com.revpay.repository;

import com.revpay.enums.InvoiceStatus;
import com.revpay.model.Invoice;
import com.revpay.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Invoice> findByUserAndStatusOrderByCreatedAtDesc(
            User user, InvoiceStatus status, Pageable pageable);

    // Search by customer name, email, or invoice number
    @Query("""
        SELECT i FROM Invoice i
        WHERE i.user = :user
        AND (
            LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(i.customerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY i.createdAt DESC
        """)
    Page<Invoice> findByUserAndSearch(@Param("user") User user, @Param("search") String search, Pageable pageable);

    Optional<Invoice> findByIdAndUser(Long id, User user);

    // Analytics — totals by status
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.user = :user AND i.status = :status")
    BigDecimal sumTotalAmountByUserAndStatus(@Param("user") User user, @Param("status") InvoiceStatus status);

    long countByUserAndStatus(User user, InvoiceStatus status);

    // Auto-generate invoice number
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user = :user")
    long countByUser(@Param("user") User user);
}