package com.routemate.backend.trip.service;

import com.routemate.backend.common.exception.BusinessRuleViolationException;
import com.routemate.backend.common.exception.ResourceNotFoundException;
import com.routemate.backend.notification.model.NotificationType;
import com.routemate.backend.notification.service.NotificationService;
import com.routemate.backend.trip.dto.AccountLockStatusDto;
import com.routemate.backend.trip.dto.CancellationPenaltyDto;
import com.routemate.backend.trip.dto.CooldownStatusDto;
import com.routemate.backend.trip.model.*;
import com.routemate.backend.trip.repository.CancellationCooldownRepository;
import com.routemate.backend.trip.repository.CancellationPenaltyRepository;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for cancellation cooldowns, penalty assessment, and account locking.
 *
 * Penalty time windows:
 *   - ≤ 1 hour since acceptance:  NO PENALTY (grace period)
 *   - 1h < elapsed < 24h:         NO PENALTY (buffer zone)
 *   - ≥ 24 hours since acceptance: PENALTY (30% of split amount)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationService {

    private static final Duration GRACE_PERIOD = Duration.ofHours(1);
    private static final Duration PENALTY_THRESHOLD = Duration.ofHours(24);
    private static final double PENALTY_RATE = 0.30;

    private final CancellationCooldownRepository cooldownRepository;
    private final CancellationPenaltyRepository penaltyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // --- Cooldown Management ---

    /**
     * Check if a user has an active cooldown on a specific trip.
     */
    @Transactional(readOnly = true)
    public boolean hasCooldown(Long userId, Long tripId) {
        return cooldownRepository.hasActiveCooldown(userId, tripId);
    }

    /**
     * Get cooldown status for a user on a specific trip.
     */
    @Transactional(readOnly = true)
    public CooldownStatusDto getCooldownStatus(Long userId, Long tripId) {
        return cooldownRepository
                .findFirstByUserIdAndTripIdAndCooldownUntilAfterOrderByCooldownUntilDesc(
                        userId, tripId, Instant.now())
                .map(cd -> new CooldownStatusDto(
                        true,
                        cd.getCooldownUntil().toString(),
                        Duration.between(Instant.now(), cd.getCooldownUntil()).toMinutes()))
                .orElse(new CooldownStatusDto(false, null, 0));
    }

    /**
     * Create a 30-minute cooldown for a user on a specific trip.
     * Called when a requester cancels/leaves a trip.
     */
    @Transactional
    public void createCooldown(User user, Trip trip) {
        CancellationCooldown cooldown = CancellationCooldown.create(user, trip);
        cooldownRepository.save(cooldown);
        log.info("Cooldown created for user {} on trip {} until {}",
                user.getEmail(), trip.getId(), cooldown.getCooldownUntil());
    }

    /**
     * Enforce cooldown check — throws if user is in cooldown for this trip.
     */
    @Transactional(readOnly = true)
    public void enforceCooldown(Long userId, Long tripId) {
        cooldownRepository
                .findFirstByUserIdAndTripIdAndCooldownUntilAfterOrderByCooldownUntilDesc(
                        userId, tripId, Instant.now())
                .ifPresent(cd -> {
                    long remainingMinutes = Duration.between(Instant.now(), cd.getCooldownUntil()).toMinutes();
                    throw new BusinessRuleViolationException(
                            "You are on a 30-minute cooldown for this trip. " +
                            "You can request again in " + remainingMinutes + " minutes. " +
                            "You may browse and request other trips in the meantime.");
                });
    }

    // --- Penalty Assessment ---

    /**
     * Assess whether a cancellation penalty applies and create it if so.
     *
     * @param trip           the trip being cancelled from
     * @param cancellingUser the user who is cancelling
     * @param otherParty     the other party (creator or requester) receiving compensation
     * @param acceptedAt     when the request was originally accepted
     * @param splitAmount    the user's cost share
     * @return true if a penalty was assessed, false if within grace period
     */
    @Transactional
    public boolean assessPenalty(Trip trip, User cancellingUser, User otherParty,
                                  Instant acceptedAt, int splitAmount) {
        if (acceptedAt == null || splitAmount <= 0) {
            log.info("No penalty: acceptedAt or splitAmount is null/zero for trip {}", trip.getId());
            return false;
        }

        Duration elapsed = Duration.between(acceptedAt, Instant.now());

        // Grace period: ≤ 1 hour
        if (elapsed.compareTo(GRACE_PERIOD) <= 0) {
            log.info("No penalty for user {} on trip {}: within 1-hour grace period (elapsed={})",
                    cancellingUser.getEmail(), trip.getId(), elapsed);
            return false;
        }

        // Buffer zone: 1h < elapsed < 24h — no penalty
        if (elapsed.compareTo(PENALTY_THRESHOLD) < 0) {
            log.info("No penalty for user {} on trip {}: within buffer zone (elapsed={})",
                    cancellingUser.getEmail(), trip.getId(), elapsed);
            return false;
        }

        // Penalty applies: ≥ 24 hours
        CancellationPenalty penalty = CancellationPenalty.create(
                trip, cancellingUser, otherParty, splitAmount, acceptedAt);
        penaltyRepository.save(penalty);

        // Lock the user's account
        cancellingUser.lockForPenalty();
        userRepository.save(cancellingUser);

        log.warn("PENALTY assessed: user={}, trip={}, amount={}, split={}",
                cancellingUser.getEmail(), trip.getId(),
                penalty.getPenaltyAmount(), splitAmount);

        // Notify the penalized user
        notificationService.createNotification(
                cancellingUser, otherParty, trip, null,
                NotificationType.CANCELLATION_PENALTY,
                "Cancellation Penalty Assessed",
                "A penalty of ₹" + penalty.getPenaltyAmount() +
                        " has been assessed for cancelling the " +
                        trip.getStartingLocation() + " → " + trip.getDestination() + " trip.");

        // Notify about account lock
        notificationService.createNotification(
                cancellingUser, null, trip, null,
                NotificationType.ACCOUNT_LOCKED,
                "Account Restricted",
                "Your account has been restricted until the cancellation penalty of ₹" +
                        penalty.getPenaltyAmount() + " is settled.");

        return true;
    }

    // --- Account Lock ---

    /**
     * Check if a user's account is locked due to unpaid penalties.
     */
    @Transactional(readOnly = true)
    public boolean isAccountLocked(Long userId) {
        return userRepository.findById(userId)
                .map(User::isPenaltyLocked)
                .orElse(false);
    }

    /**
     * Enforce account lock — throws if user has unpaid penalties.
     */
    @Transactional(readOnly = true)
    public void enforceAccountLock(User user) {
        if (user.isPenaltyLocked()) {
            long unpaidCount = penaltyRepository.countByPenalizedUserIdAndStatus(
                    user.getId(), PenaltyStatus.PENDING);
            throw new BusinessRuleViolationException(
                    "Your account is locked due to " + unpaidCount +
                    " unpaid cancellation penalty/penalties. " +
                    "Please settle your penalties before booking new trips.");
        }
    }

    /**
     * Get account lock status for a user.
     */
    @Transactional(readOnly = true)
    public AccountLockStatusDto getAccountLockStatus(User user) {
        if (!user.isPenaltyLocked()) {
            return new AccountLockStatusDto(false, null, 0, 0);
        }

        List<CancellationPenalty> unpaid = penaltyRepository.findByPenalizedUserIdAndStatus(
                user.getId(), PenaltyStatus.PENDING);
        int totalOwed = unpaid.stream().mapToInt(CancellationPenalty::getPenaltyAmount).sum();

        return new AccountLockStatusDto(
                true,
                "UNPAID_CANCELLATION_PENALTY",
                unpaid.size(),
                totalOwed);
    }

    // --- Penalty Payment ---

    /**
     * Get all penalties for a user.
     */
    @Transactional(readOnly = true)
    public List<CancellationPenaltyDto> getUserPenalties(Long userId) {
        return penaltyRepository.findByPenalizedUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Mark a penalty as paid and unlock the account if no remaining penalties.
     */
    @Transactional
    public CancellationPenaltyDto payPenalty(UUID penaltyPublicId, Long userId) {
        CancellationPenalty penalty = penaltyRepository.findByPublicId(penaltyPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty", penaltyPublicId));

        if (!penalty.getPenalizedUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("You can only pay your own penalties");
        }

        if (!penalty.isPending()) {
            throw new BusinessRuleViolationException(
                    "This penalty is already " + penalty.getStatus());
        }

        penalty.markPaid();
        penaltyRepository.save(penalty);
        log.info("Penalty {} paid by user {}", penaltyPublicId, userId);

        // Check if there are remaining unpaid penalties
        long remaining = penaltyRepository.countByPenalizedUserIdAndStatus(
                userId, PenaltyStatus.PENDING);

        if (remaining == 0) {
            User user = penalty.getPenalizedUser();
            user.unlockFromPenalty();
            userRepository.save(user);
            log.info("Account unlocked for user {} — no remaining penalties", user.getEmail());

            // Notify about account unlock
            notificationService.createNotification(
                    user, null, null, null,
                    NotificationType.ACCOUNT_UNLOCKED,
                    "Account Unlocked! ✅",
                    "Your account has been unlocked. You can now book and join trips again.");
        }

        return toDto(penalty);
    }

    private CancellationPenaltyDto toDto(CancellationPenalty penalty) {
        Trip trip = penalty.getTrip();
        String tripRoute = trip.getStartingLocation() + " → " + trip.getDestination();
        return new CancellationPenaltyDto(
                penalty.getPublicId().toString(),
                String.valueOf(trip.getId()),
                tripRoute,
                penalty.getPenaltyAmount(),
                penalty.getSplitAmount(),
                penalty.getStatus().name(),
                penalty.getCancelledAt() != null ? penalty.getCancelledAt().toString() : null,
                penalty.getAcceptedAt() != null ? penalty.getAcceptedAt().toString() : null,
                penalty.getPaidAt() != null ? penalty.getPaidAt().toString() : null);
    }
}
