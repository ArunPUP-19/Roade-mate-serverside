package com.routemate.backend.riderequest.dto;

/**
 * Ride request data returned in API responses.
 * Field names match the existing Express.js response format.
 */
public record RideRequestDto(
    String id,
    String passengerName,
    Double passengerRating,
    String startingLocation,
    String destination,
    String date,
    String preferredTime
) {}
