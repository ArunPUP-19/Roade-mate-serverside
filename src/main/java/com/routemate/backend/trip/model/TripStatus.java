package com.routemate.backend.trip.model;

/**
 * Lifecycle status of a trip (ride offer).
 *
 * Valid transitions (3-step confirmation workflow):
 *   ACTIVE → PENDING_CONFIRMATION → AWAITING_MUTUAL_CONFIRM → CONFIRMED → LOCKED → LIVE → COMPLETED → CLOSED
 *   Any non-terminal state → CANCELLED
 *   Any state → DELETED (soft delete)
 *
 * Discovery board visibility:
 *   Trip is visible on "Find My Trip" while status is ACTIVE, PENDING_CONFIRMATION, or AWAITING_MUTUAL_CONFIRM.
 *   Removed from discovery once CONFIRMED (both parties accepted in chat).
 *
 * Chat is available from AWAITING_MUTUAL_CONFIRM through COMPLETED (read-only in COMPLETED).
 * Users cannot create a new trip until their current trip is CLOSED or CANCELLED.
 */
public enum TripStatus {
    /** Trip is published and accepting join requests. */
    ACTIVE,
    /** At least one participant; waiting for organizer to accept/reject. */
    PENDING_CONFIRMATION,
    /** Creator accepted a request; two-way chat confirmation in progress. Still visible on discovery board. */
    AWAITING_MUTUAL_CONFIRM,
    /** Both creator and requester confirmed in chat. Removed from discovery board. Waiting for departure. */
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
