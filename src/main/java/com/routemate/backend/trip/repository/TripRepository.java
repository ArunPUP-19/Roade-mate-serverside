package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.Trip;
import com.routemate.backend.trip.model.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Trip entities.
 */
@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    /**
     * Search trips with optional filters on from, to, and date.
     * Matches the Express.js GET /api/trips?from=&to=&date= behavior.
     * Uses case-insensitive LIKE matching for locations.
     */
    @Query("""
        SELECT t FROM Trip t
        WHERE t.deletedAt IS NULL
          AND t.status IN ('ACTIVE', 'PENDING_CONFIRMATION')
          AND (:from IS NULL OR LOWER(t.startingLocation) LIKE LOWER(CONCAT('%', :from, '%')))
          AND (:to IS NULL OR LOWER(t.destination) LIKE LOWER(CONCAT('%', :to, '%')))
          AND (:date IS NULL OR t.tripDate = :date)
        ORDER BY t.createdAt DESC
        """)
    List<Trip> searchTrips(
        @Param("from") String from,
        @Param("to") String to,
        @Param("date") String date
    );

    List<Trip> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(TripStatus status);
}
