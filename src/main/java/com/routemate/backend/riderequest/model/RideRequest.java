package com.routemate.backend.riderequest.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the ride_request table.
 * Represents a passenger's request for a ride.
 */
@Entity
@Table(name = "ride_request")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"passenger"})
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(name = "passenger_name", nullable = false, length = 100)
    private String passengerName;

    @Column(name = "passenger_rating", nullable = false)
    private Double passengerRating = 5.0;

    @Column(name = "starting_location", nullable = false, length = 500)
    private String startingLocation;

    @Column(nullable = false, length = 500)
    private String destination;

    @Column(name = "request_date", nullable = false, length = 20)
    private String requestDate;

    @Column(name = "preferred_time", length = 20)
    private String preferredTime = "Flexible";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Integer version;

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
     * Factory method to create a new ride request.
     */
    public static RideRequest create(User passenger, String startingLocation,
                                      String destination, String date, String preferredTime) {
        RideRequest request = new RideRequest();
        request.setPassenger(passenger);
        request.setPassengerName(passenger.getDisplayName());
        request.setPassengerRating(passenger.getRating());
        request.setStartingLocation(startingLocation);
        request.setDestination(destination);
        request.setRequestDate(date);
        request.setPreferredTime(preferredTime != null ? preferredTime : "Flexible");
        return request;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = RequestStatus.DELETED;
    }
}
