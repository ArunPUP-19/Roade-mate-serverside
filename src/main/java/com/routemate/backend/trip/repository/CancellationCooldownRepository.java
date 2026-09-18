package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.CancellationCooldown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Data access layer for CancellationCooldown entities.
 */
@Repository
public interface CancellationCooldownRepository extends JpaRepository<CancellationCooldown, Long> {

    /**
     * Find an active cooldown for a specific user on a specific trip.
     * Returns a cooldown only if cooldown_until is still in the future.
     */
    Optional<CancellationCooldown> findFirstByUserIdAndTripIdAndCooldownUntilAfterOrderByCooldownUntilDesc(
            Long userId, Long tripId, Instant now);

    /**
     * Check if a user has any active cooldown on a specific trip.
     */
    default boolean hasActiveCooldown(Long userId, Long tripId) {
        return findFirstByUserIdAndTripIdAndCooldownUntilAfterOrderByCooldownUntilDesc(
                userId, tripId, Instant.now()).isPresent();
    }
}
