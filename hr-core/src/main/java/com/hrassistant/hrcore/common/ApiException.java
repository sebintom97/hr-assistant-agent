package com.hrassistant.hrcore.common;

import org.springframework.http.HttpStatus;

/**
 * Base class for errors we expect and want to show to the caller.
 * {@code code} is a stable, machine readable reason (e.g. INSUFFICIENT_BALANCE) that the
 * agent can react to; {@code message} is for humans.
 * Anything that is NOT an ApiException is a bug and becomes a 500.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    /** 401: we don't know who is calling. */
    public static class Unauthenticated extends ApiException {
        public Unauthenticated(String message) {
            super(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", message);
        }
    }

    /** 404: doesn't exist, or exists in another tenant (we never reveal which). */
    public static class NotFound extends ApiException {
        public NotFound(String what) {
            super(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found");
        }
    }

    /** 409: clashes with the current state of the data (overlap, invalid transition, concurrent edit). */
    public static class Conflict extends ApiException {
        public Conflict(String code, String message) {
            super(HttpStatus.CONFLICT, code, message);
        }
    }

    /** 422: the request is well formed but breaks a business rule (not enough balance, date in the past). */
    public static class BusinessRule extends ApiException {
        public BusinessRule(String code, String message) {
            super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
        }
    }
}
