package com.routemate.backend.notification.dto;

/**
 * Notification data returned to the frontend.
 */
public record NotificationDto(
    String id,
    String senderId,
    String senderName,
    String tripId,
    String participantPublicId,
    String type,
    String title,
    String message,
    boolean isRead,
    String createdAt
) {}
