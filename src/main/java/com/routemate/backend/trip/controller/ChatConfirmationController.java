package com.routemate.backend.trip.controller;

import com.routemate.backend.trip.dto.ChatConfirmationStatusDto;
import com.routemate.backend.trip.service.ChatConfirmationService;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for the two-way chat confirmation workflow (Steps 2 & 3).
 *
 * After the trip creator accepts a request (Step 1), both parties use these
 * endpoints to independently confirm in the trip chat. Once both confirm,
 * the trip transitions to CONFIRMED and is removed from the discovery board.
 */
@RestController
@RequestMapping("/api/trips/{tripId}/chat-confirm")
@RequiredArgsConstructor
public class ChatConfirmationController {

    private final ChatConfirmationService chatConfirmationService;
    private final UserRepository userRepository;

    /**
     * POST /api/trips/{tripId}/chat-confirm/creator
     * Creator taps "Accept" in the trip chat (Step 2a).
     */
    @PostMapping("/creator")
    public ResponseEntity<Map<String, Object>> confirmAsCreator(@PathVariable Long tripId) {
        User user = getCurrentUser();
        ChatConfirmationStatusDto status = chatConfirmationService.confirmAsCreator(tripId, user);
        return ResponseEntity.ok(Map.of(
                "message", status.fullyConfirmed()
                        ? "Trip fully confirmed! Both parties have accepted."
                        : "Your confirmation recorded. Waiting for the requester to confirm.",
                "confirmation", status
        ));
    }

    /**
     * POST /api/trips/{tripId}/chat-confirm/requester
     * Requester taps "Accept" in the trip chat (Step 2b).
     */
    @PostMapping("/requester")
    public ResponseEntity<Map<String, Object>> confirmAsRequester(@PathVariable Long tripId) {
        User user = getCurrentUser();
        ChatConfirmationStatusDto status = chatConfirmationService.confirmAsRequester(tripId, user);
        return ResponseEntity.ok(Map.of(
                "message", status.fullyConfirmed()
                        ? "Trip fully confirmed! Both parties have accepted."
                        : "Your confirmation recorded. Waiting for the creator to confirm.",
                "confirmation", status
        ));
    }

    /**
     * GET /api/trips/{tripId}/chat-confirm/status
     * Get current chat confirmation state (both booleans).
     */
    @GetMapping("/status")
    public ResponseEntity<ChatConfirmationStatusDto> getConfirmationStatus(@PathVariable Long tripId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(chatConfirmationService.getConfirmationStatus(tripId, user));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
