package com.routemate.backend.trip.mapper;

import com.routemate.backend.trip.dto.ParticipantDto;
import com.routemate.backend.trip.dto.TripDetailDto;
import com.routemate.backend.trip.dto.TripDto;
import com.routemate.backend.trip.model.Trip;
import com.routemate.backend.trip.model.TripParticipant;

import java.time.Instant;
import java.util.List;

/**
 * Maps Trip entities to TripDto records.
 * Static utility — no instantiation needed.
 */
public final class TripMapper {

    private TripMapper() {}

    /**
     * Convert a Trip entity to a TripDto (list view).
     * Field names are mapped to match the Express.js response format.
     */
    public static TripDto toDto(Trip entity) {
        return new TripDto(
            String.valueOf(entity.getId()),
            entity.getDriverName(),
            entity.getDriverRating(),
            entity.getStartingLocation(),
            entity.getDestination(),
            entity.getTripDate(),
            entity.getTripTime(),
            entity.getSeatsAvailable(),
            entity.getTotalSeats(),
            entity.getEstimatedCost(),
            entity.getTotalCost(),
            entity.getYourSplit(),
            entity.getNegotiable(),
            entity.getVehicle(),
            entity.getVehicleDetails(),
            entity.getStatus().name()
        );
    }

    /**
     * Convert a Trip entity to a TripDetailDto (detail view with participants).
     */
    public static TripDetailDto toDetailDto(Trip entity) {
        List<ParticipantDto> participantDtos = entity.getParticipants().stream()
            .map(TripMapper::toParticipantDto)
            .toList();

        return new TripDetailDto(
            String.valueOf(entity.getId()),
            entity.getDriverName(),
            entity.getDriverRating(),
            String.valueOf(entity.getDriver().getPublicId()),
            entity.getStartingLocation(),
            entity.getDestination(),
            entity.getTripDate(),
            entity.getTripTime(),
            entity.getSeatsAvailable(),
            entity.getTotalSeats(),
            entity.getEstimatedCost(),
            entity.getTotalCost(),
            entity.getYourSplit(),
            entity.getNegotiable(),
            entity.getVehicle(),
            entity.getVehicleDetails(),
            entity.getStatus().name(),
            participantDtos,
            formatInstant(entity.getCreatedAt()),
            formatInstant(entity.getLockedAt()),
            formatInstant(entity.getStartedAt()),
            formatInstant(entity.getCompletedAt())
        );
    }

    /**
     * Convert a TripParticipant entity to a ParticipantDto.
     */
    public static ParticipantDto toParticipantDto(TripParticipant participant) {
        return new ParticipantDto(
            String.valueOf(participant.getPublicId()),
            String.valueOf(participant.getUser().getPublicId()),
            participant.getUser().getDisplayName(),
            participant.getUser().getRating(),
            participant.getRole().name(),
            participant.getConfirmationStatus().name(),
            formatInstant(participant.getCreatedAt())
        );
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
