package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.CancellationPenalty;
import com.routemate.backend.trip.model.PenaltyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for CancellationPenalty entities.
 */
@Repository
public interface CancellationPenaltyRepository extends JpaRepository<CancellationPenalty, Long> {

    Optional<CancellationPenalty> findByPublicId(UUID publicId);

    List<CancellationPenalty> findByPenalizedUserIdAndStatus(Long userId, PenaltyStatus status);

    long countByPenalizedUserIdAndStatus(Long userId, PenaltyStatus status);

    List<CancellationPenalty> findByPenalizedUserIdOrderByCreatedAtDesc(Long userId);
}
