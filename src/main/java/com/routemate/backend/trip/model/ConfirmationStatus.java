package com.routemate.backend.trip.model;

/**
 * Confirmation status for a trip participant.
 *
 * 3-step workflow for passengers:
 *   PENDING → CREATOR_ACCEPTED → AWAITING_MUTUAL_CONFIRM → FULLY_CONFIRMED
 *
 * Drivers are auto-set to CONFIRMED on trip creation (legacy behavior).
 */
public enum ConfirmationStatus {
    /** Participant has requested to join; awaiting organizer decision. */
    PENDING,
    /** Step 1: Organizer has accepted the request. Chat confirm buttons will appear. */
    CREATOR_ACCEPTED,
    /** Step 2: Two-way chat confirmation in progress. Both parties have confirm buttons. */
    AWAITING_MUTUAL_CONFIRM,
    /** Step 3: Both creator and requester confirmed in chat. Trip is fully committed. */
    FULLY_CONFIRMED,
    /** Legacy: Used for driver auto-confirmation on trip creation. */
    CONFIRMED,
    /** Organizer has rejected the participant. */
    REJECTED,
    /** Participant voluntarily cancelled/left the trip. */
    CANCELLED
}

