package com.revpay.repository;

import com.revpay.model.PaymentMethod;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserOrderByIsDefaultDesc(User user);

    Optional<PaymentMethod> findByCardIdAndUser(Long cardId, User user);

    Optional<PaymentMethod> findByUserAndIsDefaultTrue(User user);

    boolean existsByUserAndIsDefaultTrue(User user);

    // Unset all default cards for a user — called before setting a new default
    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isDefault = false WHERE p.user = :user")
    void unsetAllDefaultsByUser(@Param("user") User user);

    int countByUser(User user);
}