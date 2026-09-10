package com.routemate.backend.riderequest.mapper;

import com.routemate.backend.riderequest.dto.RideRequestDto;
import com.routemate.backend.riderequest.model.RideRequest;

/**
 * Maps RideRequest entities to RideRequestDto records.
 */
public final class RideRequestMapper {

    private RideRequestMapper() {}

    public static RideRequestDto toDto(RideRequest entity) {
        return new RideRequestDto(
            String.valueOf(entity.getId()),
            entity.getPassengerName(),
            entity.getPassengerRating(),
            entity.getStartingLocation(),
            entity.getDestination(),
            entity.getRequestDate(),
            entity.getPreferredTime()
        );
    }
}
