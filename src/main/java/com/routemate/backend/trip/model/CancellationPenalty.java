package com.routemate.backend.trip.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the cancellation_penalty table.
 * Records a financial penalty when a user cancels a trip after the grace period.
 *
 * Penalty rules:
 *   - Cancellation within 1 hour of acceptance: no penalty (grace period).
 *   - Cancellation 24+ hours after acceptance: 30% of split amount owed to other party.
 *   - Unpaid penalties lock the user's account from booking new trips.
 */
@Entity
@Table(name = "cancellation_penalty")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"trip", "penalizedUser", "otherParty"})
public class CancellationPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalized_user_id", nullable = false)
    private User penalizedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "other_party_id", nullable = false)
    private User otherParty;

    /** 30% of the split amount — the fee owed. */
    @Column(name = "penalty_amount", nullable = false)
    private Integer penaltyAmount;

    /** The user's share of the trip cost (for audit trail). */
    @Column(name = "split_amount", nullable = false)
    private Integer splitAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PenaltyStatus status = PenaltyStatus.PENDING;

    /** When the cancellation occurred. */
    @Column(name = "cancelled_at", nullable = false)
    private Instant cancelledAt;

    /** When the original request was accepted (used for grace period calculation). */
    @Column(name = "accepted_at")
    private Instant acceptedAt;

    /** When the penalty was settled. */
    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Factory: create a new cancellation penalty.
     *
     * @param trip           the trip where cancellation occurred
     * @param penalizedUser  the user who cancelled
     * @param otherParty     the other party receiving compensation
     * @param splitAmount    the user's cost share for the trip
     * @param acceptedAt     when the request was originally accepted
     */
    public static CancellationPenalty create(Trip trip, User penalizedUser, User otherParty,
                                              int splitAmount, Instant acceptedAt) {
        CancellationPenalty penalty = new CancellationPenalty();
        penalty.setTrip(trip);
        penalty.setPenalizedUser(penalizedUser);
        penalty.setOtherParty(otherParty);
        penalty.setSplitAmount(splitAmount);
        penalty.setPenaltyAmount(calculatePenalty(splitAmount));
        penalty.setCancelledAt(Instant.now());
        penalty.setAcceptedAt(acceptedAt);
        return penalty;
    }

    /**
     * Calculate penalty as 30% of the split amount.
     */
    public static int calculatePenalty(int splitAmount) {
        return (int) Math.ceil(splitAmount * 0.30);
    }

    public void markPaid() {
        this.status = PenaltyStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void waive() {
        this.status = PenaltyStatus.WAIVED;
    }

    public boolean isPending() {
        return this.status == PenaltyStatus.PENDING;
    }
}
