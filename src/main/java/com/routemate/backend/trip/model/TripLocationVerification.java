package com.routemate.backend.trip.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for trip location verification.
 * Stores the GPS coordinates uploaded by each participant to verify trip completion.
 */
@Entity
@Table(name = "trip_location_verification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"trip", "user"})
public class TripLocationVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private Instant verifiedAt;

    @PrePersist
    protected void onCreate() {
        verifiedAt = Instant.now();
    }

    public static TripLocationVerification create(Trip trip, User user, Double latitude, Double longitude) {
        TripLocationVerification v = new TripLocationVerification();
        v.setTrip(trip);
        v.setUser(user);
        v.setLatitude(latitude);
        v.setLongitude(longitude);
        return v;
    }
}
