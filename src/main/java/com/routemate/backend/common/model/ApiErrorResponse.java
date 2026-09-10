package com.routemate.backend.common.model;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response for all API errors.
 */
public record ApiErrorResponse(
    String status,
    ErrorDetail error,
    ApiMeta meta
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse("error",
            new ErrorDetail(code, message, null),
            new ApiMeta(Instant.now()));
    }

    public static ApiErrorResponse of(String code, String message, List<FieldError> details) {
        return new ApiErrorResponse("error",
            new ErrorDetail(code, message, details),
            new ApiMeta(Instant.now()));
    }

    public record ErrorDetail(String code, String message, List<FieldError> details) {}
    public record FieldError(String field, String message) {}
}
