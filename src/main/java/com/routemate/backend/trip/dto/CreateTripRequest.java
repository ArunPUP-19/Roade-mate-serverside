package com.routemate.backend.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for creating a new trip (ride offer).
 * Matches the fields sent by the React frontend's POST /api/trips.
 */
public record CreateTripRequest(
    @NotBlank(message = "Starting location is required")
    String startingLocation,

    @NotBlank(message = "Destination is required")
    String destination,

    @NotBlank(message = "Date is required")
    String date,

    @NotBlank(message = "Time is required")
    String time,

    @Positive(message = "Seats must be a positive number")
    Integer seatsAvailable,

    String vehicleDetails,
    
    Integer totalCost,

    Integer yourSplit,

    Boolean negotiable
) {}
