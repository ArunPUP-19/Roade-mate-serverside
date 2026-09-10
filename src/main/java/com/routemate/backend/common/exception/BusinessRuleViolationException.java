package com.routemate.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated (e.g., duplicate trip, invalid state transition).
 */
public class BusinessRuleViolationException extends ApplicationException {

    public BusinessRuleViolationException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
