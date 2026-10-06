package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.SeismicFeedUnavailableException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Turns every error into an RFC 7807 ProblemDetail with a readable detail message.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Seconds a client should wait before retrying: the time the IGP circuit stays open. */
    static final String RETRY_AFTER_SECONDS = "60";

    @ExceptionHandler(SeismicFeedUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleFeedUnavailable(SeismicFeedUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            @NonNull MissingServletRequestParameterException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        return badRequest(exception, exception.getParameterName() + " is required", headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            @NonNull TypeMismatchException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String name = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : Objects.requireNonNullElse(exception.getPropertyName(), "parameter");
        return badRequest(exception, name + " must be " + describe(exception.getRequiredType()), headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException exception,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String detail = exception.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(exception, detail, headers, request);
    }

    private ResponseEntity<Object> badRequest(
            Exception exception,
            String detail,
            HttpHeaders headers,
            WebRequest request
    ) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        return handleExceptionInternal(exception, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String describe(Class<?> type) {
        if (type == null) {
            return "a valid value";
        }
        if (UUID.class.equals(type)) {
            return "a valid UUID";
        }
        if (Instant.class.equals(type)) {
            return "an ISO-8601 instant, for example 2026-10-06T15:30:00Z";
        }
        return "a valid " + type.getSimpleName();
    }
}
