package com.routemate.backend.trip.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for the chat_confirmation table.
 * Tracks the two-way (dual-sided) accept actions in the trip chat.
 *
 * Part of the 3-step confirmation workflow:
 *   Step 1: Creator accepts request → ChatConfirmation row created.
 *   Step 2: Both creator and requester independently tap "Accept" in chat.
 *   Step 3: When both booleans are true → trip is FULLY_CONFIRMED.
 */
@Entity
@Table(name = "chat_confirmation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"trip", "participant"})
public class ChatConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private TripParticipant participant;

    @Column(name = "creator_confirmed", nullable = false)
    private boolean creatorConfirmed = false;

    @Column(name = "creator_confirmed_at")
    private Instant creatorConfirmedAt;

    @Column(name = "requester_confirmed", nullable = false)
    private boolean requesterConfirmed = false;

    @Column(name = "requester_confirmed_at")
    private Instant requesterConfirmedAt;

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
     * Factory: create a new chat confirmation for a trip-participant pair.
     */
    public static ChatConfirmation create(Trip trip, TripParticipant participant) {
        ChatConfirmation cc = new ChatConfirmation();
        cc.setTrip(trip);
        cc.setParticipant(participant);
        return cc;
    }

    /**
     * Record creator's acceptance in chat.
     */
    public void confirmByCreator() {
        this.creatorConfirmed = true;
        this.creatorConfirmedAt = Instant.now();
    }

    /**
     * Record requester's acceptance in chat.
     */
    public void confirmByRequester() {
        this.requesterConfirmed = true;
        this.requesterConfirmedAt = Instant.now();
    }

    /**
     * Check if both parties have confirmed.
     */
    public boolean isFullyConfirmed() {
        return creatorConfirmed && requesterConfirmed;
    }
}
