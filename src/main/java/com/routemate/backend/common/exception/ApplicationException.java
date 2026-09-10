package com.routemate.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all application-specific exceptions.
 * Carries an error code and HTTP status for consistent error responses.
 */
public abstract class ApplicationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected ApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
