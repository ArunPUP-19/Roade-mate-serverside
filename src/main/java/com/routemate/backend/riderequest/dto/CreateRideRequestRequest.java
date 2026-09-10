package com.routemate.backend.riderequest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating a new ride request.
 * Matches the fields sent by the React frontend's POST /api/requests.
 */
public record CreateRideRequestRequest(
    @NotBlank(message = "Starting location is required")
    String startingLocation,

    @NotBlank(message = "Destination is required")
    String destination,

    @NotBlank(message = "Date is required")
    String date,

    String preferredTime
) {}
