package com.routemate.backend.trip.dto;

/**
 * Response DTO for cancellation penalty details.
 */
public record CancellationPenaltyDto(
    String id,
    String tripId,
    String tripRoute,
    Integer penaltyAmount,
    Integer splitAmount,
    String status,
    String cancelledAt,
    String acceptedAt,
    String paidAt
) {}
