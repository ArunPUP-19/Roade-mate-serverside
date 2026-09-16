package com.routemate.backend.notification.model;

import com.routemate.backend.trip.model.Trip;
import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the notification table.
 * Represents a notification sent to a user about trip-related events.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"recipient", "sender", "trip"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    /** Links to a TripParticipant (for "View Request" navigation). */
    @Column(name = "participant_public_id")
    private UUID participantPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Factory method to create a new notification.
     */
    public static Notification create(User recipient, User sender, Trip trip,
                                       UUID participantPublicId, NotificationType type,
                                       String title, String message) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setSender(sender);
        n.setTrip(trip);
        n.setParticipantPublicId(participantPublicId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        return n;
    }
}
