package com.routemate.backend.notification.service;

import com.routemate.backend.common.exception.BusinessRuleViolationException;
import com.routemate.backend.common.exception.ResourceNotFoundException;
import com.routemate.backend.notification.dto.NotificationDto;
import com.routemate.backend.notification.model.Notification;
import com.routemate.backend.notification.model.NotificationType;
import com.routemate.backend.notification.repository.NotificationRepository;
import com.routemate.backend.trip.model.Trip;
import com.routemate.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for user notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Create and persist a notification.
     */
    @Transactional
    public Notification createNotification(User recipient, User sender, Trip trip,
                                            UUID participantPublicId, NotificationType type,
                                            String title, String message) {
        Notification notification = Notification.create(
            recipient, sender, trip, participantPublicId, type, title, message);
        notification = notificationRepository.save(notification);
        log.info("Notification created: type={}, recipient={}, trip={}",
            type, recipient.getEmail(), trip != null ? trip.getId() : "n/a");
        return notification;
    }

    /**
     * Get all notifications for a user, newest first.
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    /**
     * Get the count of unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(UUID notificationPublicId, Long userId) {
        Notification notification = notificationRepository.findByPublicId(notificationPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationPublicId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new BusinessRuleViolationException("You can only mark your own notifications as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
            String.valueOf(n.getPublicId()),
            n.getSender() != null ? String.valueOf(n.getSender().getPublicId()) : null,
            n.getSender() != null ? n.getSender().getDisplayName() : null,
            n.getTrip() != null ? String.valueOf(n.getTrip().getId()) : null,
            n.getParticipantPublicId() != null ? String.valueOf(n.getParticipantPublicId()) : null,
            n.getType().name(),
            n.getTitle(),
            n.getMessage(),
            n.isRead(),
            n.getCreatedAt().toString()
        );
    }
}
