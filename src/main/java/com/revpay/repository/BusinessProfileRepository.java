package com.revpay.repository;

import com.revpay.model.BusinessProfile;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByUser(User user);

    boolean existsByUser(User user);
}
