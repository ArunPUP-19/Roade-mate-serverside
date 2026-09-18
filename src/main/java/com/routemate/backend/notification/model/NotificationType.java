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
    TRIP_COMPLETED,

    // --- 3-Step Confirmation Workflow ---

    /** Two-way chat confirmation is ready — both parties should confirm. */
    CHAT_CONFIRM_READY,
    /** The other party has accepted in the chat confirmation. */
    CHAT_CONFIRM_PARTNER_ACCEPTED,
    /** Both parties confirmed — trip is fully locked in. */
    TRIP_FULLY_CONFIRMED,

    // --- Cancellation & Penalty ---

    /** A cancellation penalty has been assessed. */
    CANCELLATION_PENALTY,
    /** Account has been locked due to an unpaid cancellation penalty. */
    ACCOUNT_LOCKED,
    /** Account has been unlocked after penalty was cleared. */
    ACCOUNT_UNLOCKED
}

