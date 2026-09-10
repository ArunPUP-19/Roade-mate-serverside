package com.routemate.backend.riderequest.service;

import com.routemate.backend.riderequest.dto.CreateRideRequestRequest;
import com.routemate.backend.riderequest.dto.RideRequestDto;
import com.routemate.backend.riderequest.mapper.RideRequestMapper;
import com.routemate.backend.riderequest.model.RideRequest;
import com.routemate.backend.riderequest.repository.RideRequestRepository;
import com.routemate.backend.user.model.User;
import com.routemate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for ride request operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideRequestService {

    private final RideRequestRepository rideRequestRepository;
    private final UserRepository userRepository;

    /**
     * List all active ride requests (newest first).
     */
    @Transactional(readOnly = true)
    public List<RideRequestDto> listRequests() {
        return rideRequestRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
            .stream()
            .map(RideRequestMapper::toDto)
            .toList();
    }

    /**
     * Create a new ride request for the authenticated user.
     */
    @Transactional
    public RideRequestDto createRequest(CreateRideRequestRequest request) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User passenger = userRepository.findByEmailAndDeletedAtIsNull(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        RideRequest rideRequest = RideRequest.create(
            passenger,
            request.startingLocation(),
            request.destination(),
            request.date(),
            request.preferredTime()
        );

        // Removed hardcoded "You (Requester)" logic.

        rideRequest = rideRequestRepository.save(rideRequest);
        log.info("Ride request created: id={}, from={}, to={}",
            rideRequest.getId(), rideRequest.getStartingLocation(), rideRequest.getDestination());

        return RideRequestMapper.toDto(rideRequest);
    }
}
