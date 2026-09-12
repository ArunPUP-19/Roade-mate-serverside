package com.routemate.backend.trip.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for uploading a participant's live location to verify trip completion.
 */
public record LocationUploadRequest(
    @NotNull(message = "Latitude is required")
    Double latitude,
    @NotNull(message = "Longitude is required")
    Double longitude
) {}
