package com.routemate.backend.common.model;

import java.time.Instant;

/**
 * Standard API response wrapper for all successful responses.
 */
public record ApiResponse<T>(
    String status,
    T data,
    ApiMeta meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, new ApiMeta(Instant.now()));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", data, new ApiMeta(Instant.now(), message));
    }
}
