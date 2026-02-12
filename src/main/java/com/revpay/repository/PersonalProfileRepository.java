package com.revpay.repository;

import com.revpay.model.PersonalProfile;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonalProfileRepository extends JpaRepository<PersonalProfile, Long> {

    Optional<PersonalProfile> findByUser(User user);

    boolean existsByUser(User user);

}