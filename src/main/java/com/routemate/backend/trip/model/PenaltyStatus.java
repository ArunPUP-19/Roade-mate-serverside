package com.routemate.backend.trip.model;

/**
 * Status of a cancellation penalty.
 */
public enum PenaltyStatus {
    /** Penalty assessed but not yet paid. Account is locked. */
    PENDING,
    /** Penalty has been settled by the user. */
    PAID,
    /** Admin waived the penalty. */
    WAIVED
}
