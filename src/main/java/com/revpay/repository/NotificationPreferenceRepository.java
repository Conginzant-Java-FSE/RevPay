package com.revpay.repository;

import com.revpay.enums.NotificationType;
import com.revpay.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUser(User user);

    Optional<NotificationPreference> findByUserAndNotificationType(
            User user, NotificationType notificationType);

    // Used in NotificationService to check if a type is enabled before saving
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN false ELSE true END FROM NotificationPreference p WHERE p.user = :user AND p.notificationType = :type AND p.isEnabled = false")
    boolean isNotificationEnabled(@Param("user") User user, @Param("type") NotificationType type);

    @Modifying
    @Query("DELETE FROM NotificationPreference p WHERE p.user = :user")
    void deleteByUser(@Param("user") User user);
}