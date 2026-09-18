package com.routemate.backend.trip.service;

import com.routemate.backend.common.exception.BusinessRuleViolationException;
import com.routemate.backend.notification.model.NotificationType;
import com.routemate.backend.notification.service.NotificationService;
import com.routemate.backend.trip.dto.ChatConfirmationStatusDto;
import com.routemate.backend.trip.model.*;
import com.routemate.backend.trip.repository.ChatConfirmationRepository;
import com.routemate.backend.trip.repository.TripParticipantRepository;
import com.routemate.backend.trip.repository.TripRepository;
import com.routemate.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Manages the two-way chat confirmation workflow (Step 2 → Step 3).
 *
 * After creator accepts a request (Step 1), both parties get "Accept" buttons
 * in the trip chat. When both tap Accept, the trip is FULLY_CONFIRMED and
 * removed from the discovery board.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatConfirmationService {

    private final ChatConfirmationRepository chatConfirmationRepository;
    private final TripParticipantRepository participantRepository;
    private final TripRepository tripRepository;
    private final NotificationService notificationService;

    /**
     * Initialize a chat confirmation row when the creator accepts a request.
     */
    @Transactional
    public ChatConfirmation initConfirmation(Trip trip, TripParticipant participant) {
        // Check if already exists (idempotent)
        return chatConfirmationRepository.findByTripIdAndParticipantId(trip.getId(), participant.getId())
                .orElseGet(() -> {
                    ChatConfirmation cc = ChatConfirmation.create(trip, participant);
                    cc = chatConfirmationRepository.save(cc);
                    log.info("ChatConfirmation initialized: trip={}, participant={}",
                            trip.getId(), participant.getPublicId());
                    return cc;
                });
    }

    /**
     * Creator confirms in the trip chat (Step 2a).
     */
    @Transactional
    public ChatConfirmationStatusDto confirmAsCreator(Long tripId, User creator) {
        Trip trip = findTripOrThrow(tripId);

        // Verify this user is the trip creator
        if (!trip.isOrganizer(creator)) {
            throw new BusinessRuleViolationException("Only the trip creator can confirm as creator");
        }

        // Find the active chat confirmation for this trip
        // For now, handle single-requester; find the first AWAITING_MUTUAL_CONFIRM participant
        TripParticipant participant = findAwaitingParticipant(tripId);
        ChatConfirmation cc = findChatConfirmation(tripId, participant.getId());

        if (cc.isCreatorConfirmed()) {
            throw new BusinessRuleViolationException("You have already confirmed in the chat");
        }

        cc.confirmByCreator();
        chatConfirmationRepository.save(cc);
        log.info("Creator confirmed in chat: trip={}", tripId);

        // Notify requester
        notificationService.createNotification(
                participant.getUser(), creator, trip, participant.getPublicId(),
                NotificationType.CHAT_CONFIRM_PARTNER_ACCEPTED,
                "Creator Confirmed! ✅",
                trip.getDriverName() + " has confirmed for the " +
                        trip.getStartingLocation() + " → " + trip.getDestination() + " trip. Your turn to confirm!");

        // Check if both confirmed → finalize
        if (cc.isFullyConfirmed()) {
            finalizeTrip(trip, participant, creator);
        }

        return toDto(cc, tripId);
    }

    /**
     * Requester confirms in the trip chat (Step 2b).
     */
    @Transactional
    public ChatConfirmationStatusDto confirmAsRequester(Long tripId, User requester) {
        Trip trip = findTripOrThrow(tripId);

        // Find the participant record for this requester
        TripParticipant participant = participantRepository.findByTripIdAndUserId(tripId, requester.getId())
                .orElseThrow(() -> new BusinessRuleViolationException("You are not a participant of this trip"));

        if (participant.getConfirmationStatus() != ConfirmationStatus.AWAITING_MUTUAL_CONFIRM
                && participant.getConfirmationStatus() != ConfirmationStatus.CREATOR_ACCEPTED) {
            throw new BusinessRuleViolationException(
                    "Your request is not in the confirmation phase (current: " +
                            participant.getConfirmationStatus() + ")");
        }

        ChatConfirmation cc = findChatConfirmation(tripId, participant.getId());

        if (cc.isRequesterConfirmed()) {
            throw new BusinessRuleViolationException("You have already confirmed in the chat");
        }

        cc.confirmByRequester();
        chatConfirmationRepository.save(cc);
        log.info("Requester {} confirmed in chat: trip={}", requester.getEmail(), tripId);

        // Notify creator
        notificationService.createNotification(
                trip.getDriver(), requester, trip, participant.getPublicId(),
                NotificationType.CHAT_CONFIRM_PARTNER_ACCEPTED,
                "Requester Confirmed! ✅",
                requester.getDisplayName() + " has confirmed for the " +
                        trip.getStartingLocation() + " → " + trip.getDestination() + " trip.");

        // Check if both confirmed → finalize
        if (cc.isFullyConfirmed()) {
            finalizeTrip(trip, participant, requester);
        }

        return toDto(cc, tripId);
    }

    /**
     * Get the current chat confirmation status.
     */
    @Transactional(readOnly = true)
    public ChatConfirmationStatusDto getConfirmationStatus(Long tripId, User user) {
        Trip trip = findTripOrThrow(tripId);

        // Determine the participant to check
        TripParticipant participant;
        if (trip.isOrganizer(user)) {
            participant = findAwaitingParticipant(tripId);
        } else {
            participant = participantRepository.findByTripIdAndUserId(tripId, user.getId())
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "You are not a participant of this trip"));
        }

        ChatConfirmation cc = findChatConfirmation(tripId, participant.getId());
        return toDto(cc, tripId);
    }

    // --- Internal ---

    /**
     * Finalize the trip when both parties have confirmed.
     * - Participant → FULLY_CONFIRMED
     * - Trip → CONFIRMED (removed from discovery board)
     */
    private void finalizeTrip(Trip trip, TripParticipant participant, User triggeringUser) {
        participant.fullyConfirm();
        participantRepository.save(participant);

        // Check if ALL awaiting participants are now fully confirmed
        long awaitingCount = participantRepository.countByTripIdAndConfirmationStatus(
                trip.getId(), ConfirmationStatus.AWAITING_MUTUAL_CONFIRM);
        long creatorAcceptedCount = participantRepository.countByTripIdAndConfirmationStatus(
                trip.getId(), ConfirmationStatus.CREATOR_ACCEPTED);

        if (awaitingCount == 0 && creatorAcceptedCount == 0) {
            trip.setStatus(TripStatus.CONFIRMED);
            tripRepository.save(trip);
            log.info("Trip {} FULLY CONFIRMED — removed from discovery board", trip.getId());
        }

        // Notify both parties about full confirmation
        notificationService.createNotification(
                trip.getDriver(), participant.getUser(), trip, participant.getPublicId(),
                NotificationType.TRIP_FULLY_CONFIRMED,
                "Trip Fully Confirmed! 🎉",
                "The " + trip.getStartingLocation() + " → " + trip.getDestination() +
                        " trip is now fully confirmed by both parties.");

        notificationService.createNotification(
                participant.getUser(), trip.getDriver(), trip, participant.getPublicId(),
                NotificationType.TRIP_FULLY_CONFIRMED,
                "Trip Fully Confirmed! 🎉",
                "The " + trip.getStartingLocation() + " → " + trip.getDestination() +
                        " trip is now fully confirmed by both parties.");
    }

    private TripParticipant findAwaitingParticipant(Long tripId) {
        return participantRepository.findByTripIdAndConfirmationStatus(
                        tripId, ConfirmationStatus.AWAITING_MUTUAL_CONFIRM).stream()
                .findFirst()
                .or(() -> participantRepository.findByTripIdAndConfirmationStatus(
                        tripId, ConfirmationStatus.CREATOR_ACCEPTED).stream().findFirst())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No participant is awaiting chat confirmation for this trip"));
    }

    private ChatConfirmation findChatConfirmation(Long tripId, Long participantId) {
        return chatConfirmationRepository.findByTripIdAndParticipantId(tripId, participantId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No chat confirmation found for this trip and participant. " +
                                "The creator must accept the request first."));
    }

    private Trip findTripOrThrow(Long tripId) {
        return tripRepository.findById(tripId)
                .filter(t -> t.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessRuleViolationException("Trip not found"));
    }

    private ChatConfirmationStatusDto toDto(ChatConfirmation cc, Long tripId) {
        return new ChatConfirmationStatusDto(
                String.valueOf(tripId),
                String.valueOf(cc.getParticipant().getPublicId()),
                cc.isCreatorConfirmed(),
                cc.getCreatorConfirmedAt() != null ? cc.getCreatorConfirmedAt().toString() : null,
                cc.isRequesterConfirmed(),
                cc.getRequesterConfirmedAt() != null ? cc.getRequesterConfirmedAt().toString() : null,
                cc.isFullyConfirmed());
    }
}
