package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.ConfirmationStatus;
import com.routemate.backend.trip.model.TripParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for TripParticipant entities.
 */
@Repository
public interface TripParticipantRepository extends JpaRepository<TripParticipant, Long> {

    Optional<TripParticipant> findByPublicId(UUID publicId);

    List<TripParticipant> findByTripId(Long tripId);

    List<TripParticipant> findByTripIdAndConfirmationStatus(Long tripId, ConfirmationStatus status);

    Optional<TripParticipant> findByTripIdAndUserId(Long tripId, Long userId);

    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    long countByTripIdAndConfirmationStatus(Long tripId, ConfirmationStatus status);
}
