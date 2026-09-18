package com.routemate.backend.trip.dto;

/**
 * Response DTO for the two-way chat confirmation status.
 */
public record ChatConfirmationStatusDto(
    String tripId,
    String participantId,
    boolean creatorConfirmed,
    String creatorConfirmedAt,
    boolean requesterConfirmed,
    String requesterConfirmedAt,
    boolean fullyConfirmed
) {}
