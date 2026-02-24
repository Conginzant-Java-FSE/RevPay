package com.revpay.service;

import com.revpay.dto.NotificationResponseDTO;
import com.revpay.enums.NotificationType;
import com.revpay.model.Notification;
import com.revpay.model.User;
import com.revpay.repository.NotificationPreferenceRepository;
import com.revpay.repository.NotificationRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private UserRepository userRepository;

    public List<NotificationResponseDTO> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
                .findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NotificationResponseDTO convertToDTO(Notification notification) {
        return new NotificationResponseDTO(
                notification.getNotificationId(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getCreatedAt());
    }

    @Transactional
    public void readAllNotifications() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        logger.info("Fetching notifications for user: {}", user.getEmail());

        notificationRepository.markAllAsReadByUser(user);
    }

    // ── Core method — called from any service when something notable happens ──
    @Transactional
    public void sendNotification(User user, NotificationType type, String message) {
        try {
            boolean isEnabled = notificationPreferenceRepository.isNotificationEnabled(user, type);

            if (!isEnabled) {
                logger.debug("Notification type {} is disabled for user: {}", type, user.getEmail());
                return;
            }

            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setMessage(message);
            notification.setIsRead(false);

            notificationRepository.save(notification);

            logger.debug("Notification sent to {}: [{}] {}", user.getEmail(), type, message);

        } catch (Exception e) {
            // Never let a notification failure break the main transaction
            logger.error("Failed to send notification to user {}: {}", user.getEmail(), e.getMessage());
        }
    }
    @Transactional
    public void markAsRead(Long notificationId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));


        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied: notification does not belong to this user");
        }


        if (Boolean.TRUE.equals(notification.getIsRead())) {
            logger.debug("Notification {} is already marked as read", notificationId);
            return;
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        logger.info("Notification {} marked as read for user: {}", notificationId, user.getEmail());
    }

}
