package com.routemate.backend.trip.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for the cancellation_cooldown table.
 * Tracks per-user, per-trip 30-minute re-request restrictions after cancellation.
 *
 * A user who cancels/leaves a trip request cannot re-request the same trip
 * for 30 minutes, but can request other trips freely.
 */
@Entity
@Table(name = "cancellation_cooldown")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "trip"})
public class CancellationCooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;

    @Column(name = "cooldown_until", nullable = false)
    private Instant cooldownUntil;

    /**
     * Factory: create a 30-minute cooldown for a user on a specific trip.
     */
    public static CancellationCooldown create(User user, Trip trip) {
        CancellationCooldown cooldown = new CancellationCooldown();
        cooldown.setUser(user);
        cooldown.setTrip(trip);
        cooldown.setCancelledAt(Instant.now());
        cooldown.setCooldownUntil(Instant.now().plusSeconds(30 * 60)); // 30 minutes
        return cooldown;
    }

    /**
     * Check if the cooldown is currently active.
     */
    public boolean isActive() {
        return Instant.now().isBefore(cooldownUntil);
    }
}
