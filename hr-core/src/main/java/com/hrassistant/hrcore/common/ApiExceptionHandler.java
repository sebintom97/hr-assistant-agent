package com.hrassistant.hrcore.common;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 7807 "problem details" JSON:
 * {"type":..., "title":"INSUFFICIENT_BALANCE", "status":422, "detail":"...", "code":"INSUFFICIENT_BALANCE"}
 * Extending ResponseEntityExceptionHandler gives the same format for Spring's own errors
 * (bad JSON, failed @Valid, wrong parameter type).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException e) {
        return problem(e.status(), e.code(), e.getMessage());
    }

    /** Someone else changed the row between our read and our write (JPA @Version). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "This record was changed by someone else. Reload and try again.");
    }

    /**
     * The database rejected the data. The service layer should have caught this first, so we log
     * it, but a known constraint still gets a proper answer instead of a 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        String constraint = e.getCause() instanceof ConstraintViolationException cve ? cve.getConstraintName() : null;
        log.warn("Database constraint violated: {}", constraint);
        if ("leave_request_no_overlap".equals(constraint)) {
            return problem(HttpStatus.CONFLICT, "OVERLAPPING_REQUEST",
                    "You already have a pending or approved request covering some of these days.");
        }
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "The change conflicts with existing data.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code);
        problem.setProperty("code", code);
        return problem;
    }
}
