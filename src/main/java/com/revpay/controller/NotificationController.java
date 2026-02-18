package com.revpay.controller;

import com.revpay.dto.NotificationResponseDTO;
import com.revpay.service.NotificationService;
import com.revpay.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Missing or invalid Authorization header"));
            }

            String jwt = token.substring(7);

            // Check if token is expired
            if (jwtUtil.isTokenExpired(jwt)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token has expired"));
            }

            Long userId = jwtUtil.extractUserId(jwt);
            List<NotificationResponseDTO> notifications = notificationService.getUnreadNotifications(userId);

            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token or user session: " + e.getMessage()));
        }
    }
}
