package com.routemate.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(String.format("%s not found: %s", resource, identifier),
              "NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
