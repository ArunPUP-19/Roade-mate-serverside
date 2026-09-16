package com.routemate.backend.notification.model;

/**
 * Types of notifications sent to users.
 */
public enum NotificationType {
    /** Someone requested to join your trip. */
    TRIP_REQUEST,
    /** Your join request was accepted by the organizer. */
    TRIP_REQUEST_ACCEPTED,
    /** Your join request was rejected by the organizer. */
    TRIP_REQUEST_REJECTED,
    /** A trip you are part of has started. */
    TRIP_STARTED,
    /** A trip you are part of has been completed. */
    TRIP_COMPLETED
}
