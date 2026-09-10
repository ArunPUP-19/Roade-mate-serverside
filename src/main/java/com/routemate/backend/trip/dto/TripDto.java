package com.routemate.backend.trip.dto;

/**
 * Trip data returned in API list responses.
 * Field names match the existing Express.js response format so the
 * React frontend works without any changes.
 */
public record TripDto(
    String id,
    String driverName,
    Double driverRating,
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
    String status
) {}
