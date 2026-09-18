package com.routemate.backend.trip.controller;

import com.routemate.backend.trip.dto.AccountLockStatusDto;
import com.routemate.backend.trip.dto.CancellationPenaltyDto;
import com.routemate.backend.trip.service.CancellationService;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for cancellation penalty management and account lock status.
 */
@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
public class PenaltyController {

    private final CancellationService cancellationService;
    private final UserRepository userRepository;

    /**
     * GET /api/penalties/my
     * Get all penalties for the current user.
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyPenalties() {
        User user = getCurrentUser();
        List<CancellationPenaltyDto> penalties = cancellationService.getUserPenalties(user.getId());
        AccountLockStatusDto lockStatus = cancellationService.getAccountLockStatus(user);
        return ResponseEntity.ok(Map.of(
                "penalties", penalties,
                "accountLocked", lockStatus.locked()
        ));
    }

    /**
     * POST /api/penalties/{penaltyId}/pay
     * Mark a penalty as paid. Unlocks account if no remaining penalties.
     *
     * NOTE: This is a placeholder for payment gateway integration.
     * In production, this should be triggered by a payment webhook callback.
     */
    @PostMapping("/{penaltyId}/pay")
    public ResponseEntity<Map<String, Object>> payPenalty(@PathVariable UUID penaltyId) {
        User user = getCurrentUser();
        CancellationPenaltyDto penalty = cancellationService.payPenalty(penaltyId, user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Penalty paid successfully",
                "penalty", penalty
        ));
    }

    /**
     * GET /api/account/lock-status
     * Check if the current user's account is locked.
     */
    @GetMapping("/account/lock-status")
    public ResponseEntity<AccountLockStatusDto> getAccountLockStatus() {
        User user = getCurrentUser();
        return ResponseEntity.ok(cancellationService.getAccountLockStatus(user));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
