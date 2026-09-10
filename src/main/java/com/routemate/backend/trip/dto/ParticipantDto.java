package com.routemate.backend.trip.dto;

/**
 * Participant data returned in trip detail responses.
 */
public record ParticipantDto(
    String id,
    String userId,
    String displayName,
    Double rating,
    String role,
    String confirmationStatus,
    String joinedAt
) {}
