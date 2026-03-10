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

    // ── Existing methods (keep these — used by LoanService) ──────────────────

    Page<Loan> findByUserAndStatusOrderByCreatedAtDesc(User user, LoanStatus status, Pageable pageable);

    Page<Loan> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    boolean existsByUserAndStatus(User user, LoanStatus status);

    Optional<Loan> findByLoanIdAndUser(Long loanId, User user);

    // ── New methods for AdminLoanService ──────────────────────────────────────

    // All loans newest first (admin view — no user filter)
    Page<Loan> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Filter by status newest first (admin view)
    Page<Loan> findByStatusOrderByCreatedAtDesc(LoanStatus status, Pageable pageable);

    // Dashboard stats
    long countByStatus(LoanStatus status);

    @Query("SELECT SUM(l.loanAmount) FROM Loan l WHERE l.status = :status")
    BigDecimal sumLoanAmountByStatus(@Param("status") LoanStatus status);
}