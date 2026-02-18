package com.revpay.service;

import com.revpay.dto.NotificationResponseDTO;
import com.revpay.model.Notification;
import com.revpay.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

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
}
