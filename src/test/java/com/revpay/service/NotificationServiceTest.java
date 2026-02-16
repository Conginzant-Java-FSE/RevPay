package com.revpay.service;

import com.revpay.dto.NotificationResponseDTO;
import com.revpay.model.Notification;
import com.revpay.model.User;
import com.revpay.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetUnreadNotifications() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Notification n1 = new Notification(1L, user, "Msg 1", "INFO", false, LocalDateTime.now());
        Notification n2 = new Notification(2L, user, "Msg 2", "ALERT", false, LocalDateTime.now());

        when(notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.asList(n1, n2));

        List<NotificationResponseDTO> results = notificationService.getUnreadNotifications(userId);

        assertEquals(2, results.size());
        assertEquals("Msg 1", results.get(0).getMessage());
        assertEquals("Msg 2", results.get(1).getMessage());
    }
}
