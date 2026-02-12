package com.revpay.repository;

import com.revpay.model.BankAccount;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByUser(User user);
}