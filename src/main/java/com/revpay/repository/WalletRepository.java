package com.revpay.repository;

import com.revpay.model.User;
import com.revpay.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    boolean existsByUser(User user);

    Optional<Wallet> findByUser(User user);
}
