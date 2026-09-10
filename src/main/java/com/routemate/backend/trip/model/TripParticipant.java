package com.routemate.backend.trip.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the trip_participant table.
 * Represents a user's participation in a trip with confirmation status.
 */
@Entity
@Table(name = "trip_participant")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"trip", "user"})
public class TripParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role = ParticipantRole.PASSENGER;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false, length = 20)
    private ConfirmationStatus confirmationStatus = ConfirmationStatus.PENDING;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Factory: create a DRIVER participant (auto-confirmed).
     */
    public static TripParticipant createDriver(Trip trip, User user) {
        TripParticipant p = new TripParticipant();
        p.setTrip(trip);
        p.setUser(user);
        p.setRole(ParticipantRole.DRIVER);
        p.setConfirmationStatus(ConfirmationStatus.CONFIRMED);
        p.setConfirmedAt(Instant.now());
        return p;
    }

    /**
     * Factory: create a PASSENGER participant (pending confirmation).
     */
    public static TripParticipant createPassenger(Trip trip, User user) {
        TripParticipant p = new TripParticipant();
        p.setTrip(trip);
        p.setUser(user);
        p.setRole(ParticipantRole.PASSENGER);
        p.setConfirmationStatus(ConfirmationStatus.PENDING);
        return p;
    }

    public void confirm() {
        this.confirmationStatus = ConfirmationStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void reject() {
        this.confirmationStatus = ConfirmationStatus.REJECTED;
    }

    public void cancel() {
        this.confirmationStatus = ConfirmationStatus.CANCELLED;
    }

    public boolean isConfirmed() {
        return this.confirmationStatus == ConfirmationStatus.CONFIRMED;
    }

    public boolean isPending() {
        return this.confirmationStatus == ConfirmationStatus.PENDING;
    }
}
