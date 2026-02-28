package com.revpay.controller;

import com.revpay.dto.ApiDataResponse;
import com.revpay.dto.ApiResponse;
import com.revpay.dto.NotificationPreferenceResponse;
import com.revpay.dto.NotificationResponseDTO;
import com.revpay.service.NotificationService;
import com.revpay.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@Tag(name = "Notification Management", description = "APIs for Notifications Management")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(
            summary = "Get Unread Notifications",
            description = "Fetch all unread notifications for the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Unread notifications fetched successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = NotificationResponseDTO.class)
                    )
            ),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired token",
                    content = @Content
            )

    })
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

    @Operation(
            summary = "Read All Notifications",
            description = "Mark all notifications as read for logged-in user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notifications updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @GetMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> readAllNotifications() {
        notificationService.readAllNotifications();
        ApiResponse<Void> response = new ApiResponse<>(true, "Notifications updated successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark Notification as Read",
            description = "Mark a single notification as read when the user clicks or views it. " +
                    "Only the owner of the notification can perform this action."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Notification not found or does not belong to this user"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing token"
            )
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId) {

        notificationService.markAsRead(notificationId);

        ApiResponse<Void> response =
                new ApiResponse<>(true, "Notification marked as read");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get All Notifications",
            description = "Fetch all notifications (read and unread) with pagination"
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<NotificationResponseDTO> notificationPage =
                notificationService.getAllNotifications(page, size);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "data", notificationPage.getContent(),
                        "pagination", Map.of(
                                "page", notificationPage.getNumber(),
                                "size", notificationPage.getSize(),
                                "totalElements", notificationPage.getTotalElements(),
                                "totalPages", notificationPage.getTotalPages()
                        )
                )
        );
    }

    @Operation(
            summary = "Get Notification Preferences",
            description = "Returns one entry per notification type showing enabled/disabled status."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/preferences")
    public ResponseEntity<ApiDataResponse<List<NotificationPreferenceResponse>>> getPreferences() {

        List<NotificationPreferenceResponse> data = notificationService.getPreferences();

        return ResponseEntity.ok(new ApiDataResponse<>(true, "Preferences fetched successfully", data));
    }

}
