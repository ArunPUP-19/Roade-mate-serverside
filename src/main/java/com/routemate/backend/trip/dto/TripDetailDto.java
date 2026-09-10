package com.routemate.backend.trip.dto;

import java.util.List;

/**
 * Detailed trip response including participant list and status info.
 * Used for GET /api/trips/{id} detail endpoint.
 */
public record TripDetailDto(
    String id,
    String driverName,
    Double driverRating,
    String driverId,
    String startingLocation,
    String destination,
    String date,
    String time,
    Integer seatsAvailable,
    Integer totalSeats,
    Integer estimatedCost,
    Integer totalCost,
    Integer yourSplit,
    Boolean negotiable,
    String vehicle,
    String vehicleDetails,
    String status,
    List<ParticipantDto> participants,
    String createdAt,
    String lockedAt,
    String startedAt,
    String completedAt
) {}
