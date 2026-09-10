package com.routemate.backend.trip.controller;

import com.routemate.backend.trip.dto.CreateTripRequest;
import com.routemate.backend.trip.dto.TripDetailDto;
import com.routemate.backend.trip.dto.TripDto;
import com.routemate.backend.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for trip and participant operations.
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // --- Trip CRUD ---

    /**
     * GET /api/trips?from=&to=&date=
     * Search and list trips with optional filters.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTrips(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String date) {

        List<TripDto> trips = tripService.searchTrips(from, to, date);
        return ResponseEntity.ok(Map.of("trips", trips));
    }

    /**
     * GET /api/trips/{tripId}
     * Get trip detail with participants.
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<TripDetailDto> getTripDetail(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getTripDetail(tripId));
    }

    /**
     * POST /api/trips
     * Create a new trip (offer a ride).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTrip(
            @Valid @RequestBody CreateTripRequest request) {

        TripDto trip = tripService.createTrip(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Trip published successfully!",
                "trip", trip
            ));
    }

    // --- Participant Management ---

    /**
     * POST /api/trips/{tripId}/join
     * Request to join a trip as a passenger.
     */
    @PostMapping("/{tripId}/join")
    public ResponseEntity<Map<String, Object>> joinTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.joinTrip(tripId);
        return ResponseEntity.ok(Map.of(
            "message", "Join request submitted!",
            "trip", trip
        ));
    }

    /**
     * DELETE /api/trips/{tripId}/leave
     * Leave a trip.
     */
    @DeleteMapping("/{tripId}/leave")
    public ResponseEntity<Map<String, Object>> leaveTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.leaveTrip(tripId);
        return ResponseEntity.ok(Map.of(
            "message", "You have left the trip",
            "trip", trip
        ));
    }

    /**
     * PUT /api/trips/{tripId}/participants/{participantId}/confirm
     * Organizer confirms a pending participant.
     */
    @PutMapping("/{tripId}/participants/{participantId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmParticipant(
            @PathVariable Long tripId,
            @PathVariable UUID participantId) {
        TripDetailDto trip = tripService.confirmParticipant(tripId, participantId);
        return ResponseEntity.ok(Map.of(
            "message", "Participant confirmed",
            "trip", trip
        ));
    }

    /**
     * PUT /api/trips/{tripId}/participants/{participantId}/reject
     * Organizer rejects a pending participant.
     */
    @PutMapping("/{tripId}/participants/{participantId}/reject")
    public ResponseEntity<Map<String, Object>> rejectParticipant(
            @PathVariable Long tripId,
            @PathVariable UUID participantId) {
        TripDetailDto trip = tripService.rejectParticipant(tripId, participantId);
        return ResponseEntity.ok(Map.of(
            "message", "Participant rejected",
            "trip", trip
        ));
    }

    // --- Trip Lifecycle ---

    /**
     * PUT /api/trips/{tripId}/lock
     * Lock the trip (no more participants).
     */
    @PutMapping("/{tripId}/lock")
    public ResponseEntity<Map<String, Object>> lockTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.lockTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip locked", "trip", trip));
    }

    /**
     * PUT /api/trips/{tripId}/start
     * Start the trip (transition to LIVE).
     */
    @PutMapping("/{tripId}/start")
    public ResponseEntity<Map<String, Object>> startTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.startTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip started", "trip", trip));
    }

    /**
     * PUT /api/trips/{tripId}/complete
     * Complete the trip.
     */
    @PutMapping("/{tripId}/complete")
    public ResponseEntity<Map<String, Object>> completeTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.completeTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip completed", "trip", trip));
    }

    /**
     * PUT /api/trips/{tripId}/cancel
     * Cancel the trip (organizer only).
     */
    @PutMapping("/{tripId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.cancelTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip cancelled", "trip", trip));
    }
}
