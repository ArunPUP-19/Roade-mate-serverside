package com.routemate.backend.trip.model;

/**
 * Confirmation status for a trip participant.
 */
public enum ConfirmationStatus {
    /** Participant has requested to join; awaiting organizer decision. */
    PENDING,
    /** Organizer has confirmed the participant. */
    CONFIRMED,
    /** Organizer has rejected the participant. */
    REJECTED,
    /** Participant voluntarily cancelled/left the trip. */
    CANCELLED
}
