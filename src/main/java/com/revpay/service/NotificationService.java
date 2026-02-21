package com.revpay.service;

import com.revpay.dto.NotificationResponseDTO;
import com.revpay.model.Notification;
import com.revpay.model.User;
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
                notification.getRead(),
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

}
