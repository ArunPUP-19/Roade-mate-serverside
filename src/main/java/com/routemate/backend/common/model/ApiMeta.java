package com.routemate.backend.common.model;

import java.time.Instant;

/**
 * Metadata included with every API response.
 */
public record ApiMeta(
    Instant timestamp,
    String message
) {
    public ApiMeta(Instant timestamp) {
        this(timestamp, null);
    }
}
