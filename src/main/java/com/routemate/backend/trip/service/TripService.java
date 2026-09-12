package com.routemate.backend.trip.service;

import com.routemate.backend.common.exception.BusinessRuleViolationException;
import com.routemate.backend.common.exception.ResourceNotFoundException;
import com.routemate.backend.trip.dto.CreateTripRequest;
import com.routemate.backend.trip.dto.LocationUploadRequest;
import com.routemate.backend.trip.dto.TripDetailDto;
import com.routemate.backend.trip.dto.TripDto;
import com.routemate.backend.trip.mapper.TripMapper;
import com.routemate.backend.trip.model.*;
import com.routemate.backend.trip.repository.TripLocationVerificationRepository;
import com.routemate.backend.trip.repository.TripParticipantRepository;
import com.routemate.backend.trip.repository.TripRepository;
import com.routemate.backend.trip.repository.TripMessageRepository;
import com.routemate.backend.trip.dto.TripMessageDto;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for trip (ride offer) and participant operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TripMessageRepository tripMessageRepository;
    private final TripLocationVerificationRepository locationVerificationRepository;

    // --- Trip CRUD ---

    /**
     * Search trips with optional filters.
     */
    @Transactional(readOnly = true)
    public List<TripDto> searchTrips(String from, String to, String date) {
        List<Trip> trips = tripRepository.searchTrips(
            from != null && !from.isBlank() ? from : null,
            to != null && !to.isBlank() ? to : null,
            date != null && !date.isBlank() ? date : null
        );
        return trips.stream().map(TripMapper::toDto).toList();
    }

    /**
     * Get a single trip with full detail including participants.
     */
    @Transactional(readOnly = true)
    public TripDetailDto getTripDetail(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        return TripMapper.toDetailDto(trip);
    }

    /**
     * Create a new trip and auto-register the creator as DRIVER participant.
     */
    @Transactional
    public TripDto createTrip(CreateTripRequest request) {
        User driver = getCurrentUser();

        // Block trip creation if user already has an active trip as driver
        if (tripRepository.hasActiveTripAsDriver(driver.getId())) {
            throw new BusinessRuleViolationException(
                "You already have an active trip. Please close it before creating a new one.");
        }

        Trip trip = Trip.create(
            driver,
            request.startingLocation(),
            request.destination(),
            request.date(),
            request.time(),
            request.seatsAvailable() != null ? request.seatsAvailable() : 2,
            request.vehicleDetails(),
            request.totalCost(),
            request.yourSplit(),
            request.negotiable()
        );

        trip = tripRepository.save(trip);

        // Auto-register the driver as a confirmed participant
        TripParticipant driverParticipant = TripParticipant.createDriver(trip, driver);
        participantRepository.save(driverParticipant);

        log.info("Trip created: id={}, from={}, to={}, driver={}",
            trip.getId(), trip.getStartingLocation(), trip.getDestination(), driver.getEmail());

        return TripMapper.toDto(trip);
    }

    // --- Participant Management ---

    /**
     * Request to join a trip as a passenger.
     */
    @Transactional
    public TripDetailDto joinTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();

        // Validate: can't join your own trip
        if (trip.isOrganizer(user)) {
            throw new BusinessRuleViolationException("You cannot join your own trip");
        }

        // Validate: trip must be joinable
        if (!trip.isJoinable()) {
            throw new BusinessRuleViolationException(
                "This trip is not accepting new participants (status: " + trip.getStatus() + ")");
        }

        // Validate: no duplicate join requests
        if (participantRepository.existsByTripIdAndUserId(tripId, user.getId())) {
            throw new BusinessRuleViolationException("You have already requested to join this trip");
        }

        // Create pending participant and decrement available seats
        TripParticipant participant = TripParticipant.createPassenger(trip, user);
        participantRepository.save(participant);
        trip.decrementSeats();

        // Update trip status if this is the first join request
        if (trip.getStatus() == TripStatus.ACTIVE) {
            trip.setStatus(TripStatus.PENDING_CONFIRMATION);
        }

        tripRepository.save(trip);
        log.info("User {} requested to join trip {}", user.getEmail(), tripId);

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Leave a trip (cancel participation).
     */
    @Transactional
    public TripDetailDto leaveTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();

        TripParticipant participant = participantRepository.findByTripIdAndUserId(tripId, user.getId())
            .orElseThrow(() -> new BusinessRuleViolationException("You are not a participant of this trip"));

        // Drivers can't leave their own trip (they should cancel it instead)
        if (participant.getRole() == ParticipantRole.DRIVER) {
            throw new BusinessRuleViolationException("As the driver, you cannot leave. Cancel the trip instead.");
        }

        // Can't leave a live or completed trip
        if (trip.getStatus() == TripStatus.LIVE || trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Cannot leave a trip that is " + trip.getStatus());
        }

        participant.cancel();
        trip.incrementSeats();
        tripRepository.save(trip);
        log.info("User {} left trip {}", user.getEmail(), tripId);

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Organizer confirms a pending participant.
     */
    @Transactional
    public TripDetailDto confirmParticipant(Long tripId, UUID participantPublicId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        TripParticipant participant = participantRepository.findByPublicId(participantPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant", participantPublicId));

        if (!participant.getTrip().getId().equals(tripId)) {
            throw new BusinessRuleViolationException("Participant does not belong to this trip");
        }

        if (!participant.isPending()) {
            throw new BusinessRuleViolationException(
                "Participant is not in PENDING status (current: " + participant.getConfirmationStatus() + ")");
        }

        participant.confirm();
        participantRepository.save(participant);
        log.info("Participant {} confirmed for trip {}", participantPublicId, tripId);

        // Check if all pending participants are now confirmed
        long pendingCount = participantRepository.countByTripIdAndConfirmationStatus(tripId, ConfirmationStatus.PENDING);
        if (pendingCount == 0 && trip.getStatus() == TripStatus.PENDING_CONFIRMATION) {
            trip.setStatus(TripStatus.CONFIRMED);
            tripRepository.save(trip);
            log.info("All participants confirmed for trip {} — status now CONFIRMED", tripId);
        }

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Organizer rejects a pending participant.
     */
    @Transactional
    public TripDetailDto rejectParticipant(Long tripId, UUID participantPublicId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        TripParticipant participant = participantRepository.findByPublicId(participantPublicId)
            .orElseThrow(() -> new ResourceNotFoundException("Participant", participantPublicId));

        if (!participant.getTrip().getId().equals(tripId)) {
            throw new BusinessRuleViolationException("Participant does not belong to this trip");
        }

        if (!participant.isPending()) {
            throw new BusinessRuleViolationException(
                "Participant is not in PENDING status (current: " + participant.getConfirmationStatus() + ")");
        }

        participant.reject();
        trip.incrementSeats();
        participantRepository.save(participant);
        tripRepository.save(trip);
        log.info("Participant {} rejected from trip {}", participantPublicId, tripId);

        return TripMapper.toDetailDto(trip);
    }

    // --- Trip Lifecycle Transitions ---

    /**
     * Lock the trip (no more participants, preparing for departure).
     */
    @Transactional
    public TripDetailDto lockTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        if (trip.getStatus() != TripStatus.CONFIRMED && trip.getStatus() != TripStatus.PENDING_CONFIRMATION) {
            throw new BusinessRuleViolationException("Trip must be CONFIRMED or PENDING_CONFIRMATION to lock");
        }

        // Reject any remaining pending participants
        List<TripParticipant> pendingParticipants = participantRepository
            .findByTripIdAndConfirmationStatus(tripId, ConfirmationStatus.PENDING);
        for (TripParticipant p : pendingParticipants) {
            p.reject();
            trip.incrementSeats();
        }
        participantRepository.saveAll(pendingParticipants);

        trip.lock();
        tripRepository.save(trip);
        log.info("Trip {} locked by organizer {}", tripId, organizer.getEmail());

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Start the trip (transition to LIVE).
     */
    @Transactional
    public TripDetailDto startTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        if (trip.getStatus() != TripStatus.LOCKED && trip.getStatus() != TripStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Trip must be LOCKED or CONFIRMED to start");
        }

        trip.start();
        tripRepository.save(trip);
        log.info("Trip {} started", tripId);

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Complete the trip. Requires all confirmed participants to have uploaded location.
     */
    @Transactional
    public TripDetailDto completeTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        if (trip.getStatus() != TripStatus.LIVE) {
            throw new BusinessRuleViolationException("Only LIVE trips can be completed");
        }

        // Verify all confirmed participants have uploaded their location
        long confirmedCount = participantRepository.countByTripIdAndConfirmationStatus(
            tripId, ConfirmationStatus.CONFIRMED);
        long verifiedCount = locationVerificationRepository.countByTripId(tripId);

        if (verifiedCount < confirmedCount) {
            throw new BusinessRuleViolationException(
                "All confirmed participants must upload their location before the trip can be completed. "
                + verifiedCount + "/" + confirmedCount + " verified.");
        }

        trip.complete();
        tripRepository.save(trip);
        log.info("Trip {} completed", tripId);

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Close the trip (organizer only). Terminal state that frees user to create new trips.
     */
    @Transactional
    public TripDetailDto closeTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new BusinessRuleViolationException("Only COMPLETED trips can be closed");
        }

        trip.close();
        tripRepository.save(trip);
        log.info("Trip {} closed by organizer {}", tripId, organizer.getEmail());

        return TripMapper.toDetailDto(trip);
    }

    /**
     * Cancel the trip (organizer only).
     */
    @Transactional
    public TripDetailDto cancelTrip(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User organizer = getCurrentUser();
        ensureOrganizer(trip, organizer);

        if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CLOSED
                || trip.getStatus() == TripStatus.DELETED) {
            throw new BusinessRuleViolationException("Cannot cancel a " + trip.getStatus() + " trip");
        }

        trip.cancel();
        tripRepository.save(trip);
        log.info("Trip {} cancelled by organizer {}", tripId, organizer.getEmail());

        return TripMapper.toDetailDto(trip);
    }

    // --- Location Verification ---

    /**
     * Upload current location to verify trip completion.
     */
    @Transactional
    public Map<String, Object> uploadLocation(Long tripId, LocationUploadRequest request) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();
        ensureConfirmedParticipant(tripId, user.getId());

        if (trip.getStatus() != TripStatus.LIVE) {
            throw new BusinessRuleViolationException("Location can only be uploaded for LIVE trips");
        }

        if (locationVerificationRepository.existsByTripIdAndUserId(tripId, user.getId())) {
            throw new BusinessRuleViolationException("You have already uploaded your location");
        }

        TripLocationVerification verification = TripLocationVerification.create(
            trip, user, request.latitude(), request.longitude());
        locationVerificationRepository.save(verification);
        log.info("User {} uploaded location for trip {}", user.getEmail(), tripId);

        long confirmedCount = participantRepository.countByTripIdAndConfirmationStatus(
            tripId, ConfirmationStatus.CONFIRMED);
        long verifiedCount = locationVerificationRepository.countByTripId(tripId);

        return Map.of(
            "message", "Location uploaded successfully",
            "verifiedCount", verifiedCount,
            "totalRequired", confirmedCount,
            "allVerified", verifiedCount >= confirmedCount
        );
    }

    /**
     * Get location verification status for a trip.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLocationStatus(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();
        ensureConfirmedParticipant(tripId, user.getId());

        long confirmedCount = participantRepository.countByTripIdAndConfirmationStatus(
            tripId, ConfirmationStatus.CONFIRMED);
        long verifiedCount = locationVerificationRepository.countByTripId(tripId);
        boolean currentUserVerified = locationVerificationRepository.existsByTripIdAndUserId(
            tripId, user.getId());

        return Map.of(
            "verifiedCount", verifiedCount,
            "totalRequired", confirmedCount,
            "allVerified", verifiedCount >= confirmedCount,
            "currentUserVerified", currentUserVerified
        );
    }

    /**
     * Check if current user has an active trip as driver.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveTripStatus() {
        User user = getCurrentUser();
        boolean hasActive = tripRepository.hasActiveTripAsDriver(user.getId());
        return Map.of("hasActiveTrip", hasActive);
    }

    // --- Helpers ---

    private Trip findTripOrThrow(Long tripId) {
        return tripRepository.findById(tripId)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private void ensureOrganizer(Trip trip, User user) {
        if (!trip.isOrganizer(user)) {
            throw new BusinessRuleViolationException("Only the trip organizer can perform this action");
        }
    }

    // --- Chat ---

    /**
     * Get all chat messages for a trip. Validates user is confirmed participant.
     */
    @Transactional(readOnly = true)
    public List<TripMessageDto> getTripMessages(Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();
        ensureConfirmedParticipant(tripId, user.getId());

        List<TripMessage> messages = tripMessageRepository.findByTripIdOrderByCreatedAtAsc(tripId);
        return messages.stream()
            .map(m -> TripMessageDto.builder()
                .id(m.getId())
                .senderId(m.getSender().getPublicId())
                .senderName(m.getSender().getDisplayName())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build())
            .toList();
    }

    /**
     * Send a new chat message to a trip.
     */
    @Transactional
    public TripMessageDto sendTripMessage(Long tripId, String content) {
        Trip trip = findTripOrThrow(tripId);
        User user = getCurrentUser();
        ensureConfirmedParticipant(tripId, user.getId());

        TripMessage message = TripMessage.create(trip, user, content);
        message = tripMessageRepository.save(message);

        log.info("User {} sent a message in trip {}", user.getEmail(), tripId);

        return TripMessageDto.builder()
            .id(message.getId())
            .senderId(user.getPublicId())
            .senderName(user.getDisplayName())
            .content(message.getContent())
            .createdAt(message.getCreatedAt())
            .build();
    }

    private void ensureConfirmedParticipant(Long tripId, Long userId) {
        TripParticipant participant = participantRepository.findByTripIdAndUserId(tripId, userId)
            .orElseThrow(() -> new BusinessRuleViolationException("You are not a participant of this trip"));
            
        if (participant.getConfirmationStatus() != ConfirmationStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Only confirmed participants can access trip chat");
        }
    }
}
