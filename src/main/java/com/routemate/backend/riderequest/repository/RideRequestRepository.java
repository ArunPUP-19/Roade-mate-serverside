package com.routemate.backend.riderequest.repository;

import com.routemate.backend.riderequest.model.RideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for RideRequest entities.
 */
@Repository
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    Optional<RideRequest> findByPublicIdAndDeletedAtIsNull(UUID publicId);

    List<RideRequest> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
