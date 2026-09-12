package com.routemate.backend.trip.model;

/**
 * Lifecycle status of a trip (ride offer).
 * Valid transitions:
 *   ACTIVE → PENDING_CONFIRMATION → CONFIRMED → LOCKED → LIVE → COMPLETED → CLOSED
 *   Any non-terminal state → CANCELLED
 *   Any state → DELETED (soft delete)
 *
 * Chat is available from CONFIRMED through COMPLETED (read-only in COMPLETED).
 * Users cannot create a new trip until their current trip is CLOSED or CANCELLED.
 */
public enum TripStatus {
    /** Trip is published and accepting join requests. */
    ACTIVE,
    /** At least one participant; waiting for organizer to confirm all. */
    PENDING_CONFIRMATION,
    /** All participants confirmed; waiting for departure time. */
    CONFIRMED,
    /** Locked within 30 min of departure; no new participants. */
    LOCKED,
    /** Trip is underway; location tracking active. */
    LIVE,
    /** Trip has no more available seats. */
    FULL,
    /** Trip completed successfully; all locations verified. */
    COMPLETED,
    /** Trip formally closed by organizer; terminal state. Frees user to create new trips. */
    CLOSED,
    /** Trip cancelled by organizer. */
    CANCELLED,
    /** Soft-deleted. */
    DELETED
}
