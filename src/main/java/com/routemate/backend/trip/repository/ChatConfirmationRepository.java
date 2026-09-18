package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.ChatConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for ChatConfirmation entities.
 */
@Repository
public interface ChatConfirmationRepository extends JpaRepository<ChatConfirmation, Long> {

    Optional<ChatConfirmation> findByTripIdAndParticipantId(Long tripId, Long participantId);

    List<ChatConfirmation> findByTripId(Long tripId);
}
