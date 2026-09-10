package com.routemate.backend.riderequest.controller;

import com.routemate.backend.riderequest.dto.CreateRideRequestRequest;
import com.routemate.backend.riderequest.dto.RideRequestDto;
import com.routemate.backend.riderequest.service.RideRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for ride request operations.
 * Endpoint paths match the existing Express.js routes exactly.
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RideRequestController {

    private final RideRequestService rideRequestService;

    /**
     * GET /api/requests
     * List all ride requests.
     *
     * Response format matches Express.js: { "requests": [...] }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listRequests() {
        List<RideRequestDto> requests = rideRequestService.listRequests();
        return ResponseEntity.ok(Map.of("requests", requests));
    }

    /**
     * POST /api/requests
     * Create a new ride request.
     *
     * Response format matches Express.js: { "message": "...", "request": {...} }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRequest(
            @Valid @RequestBody CreateRideRequestRequest request) {

        RideRequestDto rideRequest = rideRequestService.createRequest(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Ride request published successfully!",
                "request", rideRequest
            ));
    }
}
