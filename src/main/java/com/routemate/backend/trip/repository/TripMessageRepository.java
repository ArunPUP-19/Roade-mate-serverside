package com.routemate.backend.trip.repository;

import com.routemate.backend.trip.model.TripMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripMessageRepository extends JpaRepository<TripMessage, Long> {
    List<TripMessage> findByTripIdOrderByCreatedAtAsc(Long tripId);
}
