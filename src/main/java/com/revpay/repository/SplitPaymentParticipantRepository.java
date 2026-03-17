package com.revpay.repository;

import com.revpay.model.SplitPayment;
import com.revpay.model.SplitPaymentParticipant;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitPaymentParticipantRepository extends JpaRepository<SplitPaymentParticipant, Long> {
    List<SplitPaymentParticipant> findByUserAndPaidFalse(User user);
    List<SplitPaymentParticipant> findBySplit(SplitPayment split);
}
