package com.routemate.backend.riderequest.model;

/**
 * Lifecycle status of a ride request.
 */
public enum RequestStatus {
    OPEN,
    MATCHED,
    COMPLETED,
    CANCELLED,
    DELETED
}
