package com.routemate.backend.notification.controller;

import com.routemate.backend.notification.dto.NotificationDto;
import com.routemate.backend.notification.service.NotificationService;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * GET /api/notifications
     * List all notifications for the current user.
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        User user = getCurrentUser();
        return ResponseEntity.ok(notificationService.getNotifications(user.getId()));
    }

    /**
     * GET /api/notifications/unread-count
     * Get the number of unread notifications.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        User user = getCurrentUser();
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * PATCH /api/notifications/{notificationId}/read
     * Mark a single notification as read.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable UUID notificationId) {
        User user = getCurrentUser();
        notificationService.markAsRead(notificationId, user.getId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * PATCH /api/notifications/read-all
     * Mark all notifications as read.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
