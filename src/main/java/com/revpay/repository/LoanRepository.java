package com.revpay.repository;

import com.revpay.enums.LoanStatus;
import com.revpay.model.Loan;
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
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Page<Loan> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Loan> findByUserAndStatusOrderByCreatedAtDesc(
            User user, LoanStatus status, Pageable pageable);

    Optional<Loan> findByLoanIdAndUser(Long loanId, User user);

    // Check if user has any active loan before allowing a new application
    boolean existsByUserAndStatus(User user, LoanStatus status);

    // Total outstanding balance across all active loans for a user
    @Query("SELECT COALESCE(SUM(l.outstandingBalance), 0) FROM Loan l WHERE l.user = :user AND l.status = 'ACTIVE'")
    BigDecimal sumOutstandingBalanceByUser(@Param("user") User user);
}