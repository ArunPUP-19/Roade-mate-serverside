package com.routemate.backend.trip.dto;

/**
 * Response DTO for account lock status.
 */
public record AccountLockStatusDto(
    boolean locked,
    String reason,
    long unpaidPenalties,
    int totalOwed
) {}
