package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.TripLocationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for TripLocationVerification entities.
 */
@Repository
public interface TripLocationVerificationRepository extends JpaRepository<TripLocationVerification, Long> {

    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    long countByTripId(Long tripId);

    List<TripLocationVerification> findByTripId(Long tripId);
}
