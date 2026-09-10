package com.routemate.backend.trip.model;

import com.routemate.backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for the trip table.
 * Represents a ride offer published by a driver.
 */
@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"driver", "participants"})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "driver_name", nullable = false, length = 100)
    private String driverName;

    @Column(name = "driver_rating", nullable = false)
    private Double driverRating = 5.0;

    @Column(name = "starting_location", nullable = false, length = 500)
    private String startingLocation;

    @Column(nullable = false, length = 500)
    private String destination;

    @Column(name = "trip_date", nullable = false, length = 20)
    private String tripDate;

    @Column(name = "trip_time", nullable = false, length = 20)
    private String tripTime;

    @Column(name = "seats_available", nullable = false)
    private Integer seatsAvailable;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "estimated_cost", nullable = false)
    private Integer estimatedCost = 0;

    @Column(name = "total_cost")
    private Integer totalCost;

    @Column(name = "your_split")
    private Integer yourSplit;

    @Column(name = "negotiable")
    private Boolean negotiable;

    @Column(length = 200)
    private String vehicle;

    @Column(name = "vehicle_details", length = 500)
    private String vehicleDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TripStatus status = TripStatus.ACTIVE;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "data_delete_at")
    private Instant dataDeleteAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripParticipant> participants = new ArrayList<>();

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
     * Factory method to create a new trip (ride offer).
     */
    public static Trip create(User driver, String startingLocation, String destination,
                               String date, String time, int seatsAvailable,
                               String vehicleDetails, Integer totalCost, Integer yourSplit, Boolean negotiable) {
        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setDriverName(driver.getDisplayName());
        trip.setDriverRating(driver.getRating());
        trip.setStartingLocation(startingLocation);
        trip.setDestination(destination);
        trip.setTripDate(date);
        trip.setTripTime(time);
        trip.setSeatsAvailable(seatsAvailable);
        trip.setTotalSeats(seatsAvailable);
        
        // Use provided totalCost or default to 0
        trip.setTotalCost(totalCost);
        trip.setEstimatedCost(totalCost != null ? totalCost : 0);
        
        trip.setYourSplit(yourSplit);
        trip.setNegotiable(negotiable != null ? negotiable : false);
        trip.setVehicleDetails(vehicleDetails);
        trip.setVehicle(vehicleDetails != null ? vehicleDetails : "Personal Vehicle");
        
        return trip;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.status = TripStatus.DELETED;
    }

    // --- State transition helpers ---

    public boolean isJoinable() {
        return (status == TripStatus.ACTIVE || status == TripStatus.PENDING_CONFIRMATION)
                && seatsAvailable > 0;
    }

    public void decrementSeats() {
        if (seatsAvailable <= 0) {
            throw new IllegalStateException("No seats available");
        }
        this.seatsAvailable--;
        if (this.seatsAvailable == 0) {
            this.status = TripStatus.FULL;
        }
    }

    public void incrementSeats() {
        this.seatsAvailable++;
        if (this.status == TripStatus.FULL) {
            // Restore to PENDING_CONFIRMATION if there are still other pending participants,
            // otherwise back to ACTIVE
            this.status = TripStatus.PENDING_CONFIRMATION;
        }
    }

    public void lock() {
        this.status = TripStatus.LOCKED;
        this.lockedAt = Instant.now();
    }

    public void start() {
        this.status = TripStatus.LIVE;
        this.startedAt = Instant.now();
    }

    public void complete() {
        this.status = TripStatus.COMPLETED;
        this.completedAt = Instant.now();
        // Schedule data deletion 30 days after completion
        this.dataDeleteAt = this.completedAt.plusSeconds(30L * 24 * 60 * 60);
    }

    public void cancel() {
        this.status = TripStatus.CANCELLED;
    }

    public boolean isOrganizer(User user) {
        return this.driver.getId().equals(user.getId());
    }

    public long getConfirmedParticipantCount() {
        return participants.stream()
                .filter(TripParticipant::isConfirmed)
                .count();
    }
}
