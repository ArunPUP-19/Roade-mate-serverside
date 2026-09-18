package com.routemate.backend.trip.dto;

/**
 * Response DTO for cooldown status on a specific trip.
 */
public record CooldownStatusDto(
    boolean onCooldown,
    String cooldownUntil,
    long remainingMinutes
) {}
