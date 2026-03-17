package com.revpay.repository;

import com.revpay.model.SplitPayment;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitPaymentRepository extends JpaRepository<SplitPayment, Long> {
    List<SplitPayment> findByCreatedByUserOrderByCreatedAtDesc(User user);
}
