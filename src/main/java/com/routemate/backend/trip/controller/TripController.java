package com.routemate.backend.trip.controller;

import com.routemate.backend.trip.dto.CreateTripRequest;
import com.routemate.backend.trip.dto.CooldownStatusDto;
import com.routemate.backend.trip.dto.LocationUploadRequest;
import com.routemate.backend.trip.dto.TripDetailDto;
import com.routemate.backend.trip.dto.TripDto;
import com.routemate.backend.trip.service.CancellationService;
import com.routemate.backend.trip.service.TripService;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
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
    private final CancellationService cancellationService;
    private final UserRepository userRepository;

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
     * GET /api/trips/my-trips
     * Get trips created by the current user (as driver/organizer).
     */
    @GetMapping("/my-trips")
    public ResponseEntity<Map<String, Object>> getMyTrips() {
        return ResponseEntity.ok(Map.of("trips", tripService.getMyTrips()));
    }

    /**
     * GET /api/trips/my-requests
     * Get trips the current user has requested to join (as passenger).
     */
    @GetMapping("/my-requests")
    public ResponseEntity<Map<String, Object>> getMyRequests() {
        return ResponseEntity.ok(Map.of("trips", tripService.getMyRequests()));
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
     * Complete the trip (requires all participant locations verified).
     */
    @PutMapping("/{tripId}/complete")
    public ResponseEntity<Map<String, Object>> completeTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.completeTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip completed", "trip", trip));
    }

    /**
     * PUT /api/trips/{tripId}/close
     * Close the trip (organizer only). Terminal state.
     */
    @PutMapping("/{tripId}/close")
    public ResponseEntity<Map<String, Object>> closeTrip(@PathVariable Long tripId) {
        TripDetailDto trip = tripService.closeTrip(tripId);
        return ResponseEntity.ok(Map.of("message", "Trip closed", "trip", trip));
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

    // --- Location Verification ---

    /**
     * POST /api/trips/{tripId}/location
     * Upload live location to verify trip completion.
     */
    @PostMapping("/{tripId}/location")
    public ResponseEntity<Map<String, Object>> uploadLocation(
            @PathVariable Long tripId,
            @Valid @RequestBody LocationUploadRequest request) {
        Map<String, Object> result = tripService.uploadLocation(tripId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/trips/{tripId}/location-status
     * Check location verification status for the trip.
     */
    @GetMapping("/{tripId}/location-status")
    public ResponseEntity<Map<String, Object>> getLocationStatus(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getLocationStatus(tripId));
    }

    /**
     * GET /api/trips/my-active-status
     * Check if current user has an active trip (for creation restriction).
     */
    @GetMapping("/my-active-status")
    public ResponseEntity<Map<String, Object>> getActiveTripStatus() {
        return ResponseEntity.ok(tripService.getActiveTripStatus());
    }

    // --- Chat ---

    /**
     * GET /api/trips/{tripId}/chat
     * Get chat messages for a trip.
     */
    @GetMapping("/{tripId}/chat")
    public ResponseEntity<List<com.routemate.backend.trip.dto.TripMessageDto>> getTripMessages(@PathVariable Long tripId) {
        return ResponseEntity.ok(tripService.getTripMessages(tripId));
    }

    /**
     * POST /api/trips/{tripId}/chat
     * Send a chat message to a trip.
     */
    @PostMapping("/{tripId}/chat")
    public ResponseEntity<com.routemate.backend.trip.dto.TripMessageDto> sendTripMessage(
            @PathVariable Long tripId,
            @RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tripService.sendTripMessage(tripId, content.trim()));
    }

    // --- Cooldown Status ---

    /**
     * GET /api/trips/{tripId}/cooldown-status
     * Check if the current user has an active cooldown for this trip.
     */
    @GetMapping("/{tripId}/cooldown-status")
    public ResponseEntity<CooldownStatusDto> getCooldownStatus(@PathVariable Long tripId) {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return ResponseEntity.ok(cancellationService.getCooldownStatus(user.getId(), tripId));
    }
}
