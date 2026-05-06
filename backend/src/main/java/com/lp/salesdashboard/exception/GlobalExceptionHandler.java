package com.lp.salesdashboard.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Logs all standard Spring MVC exceptions (missing params, type mismatch, etc.)
     * before delegating to the default response builder.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String uri = ((ServletWebRequest) request).getRequest().getRequestURI();
        if (statusCode.is5xxServerError()) {
            log.error("Error {} on {}: {}", statusCode.value(), uri, ex.getMessage(), ex);
        } else {
            log.warn("Error {} on {}: {}", statusCode.value(), uri, ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    /** Overrides the default 400 body to include per-field validation details. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return handleExceptionInternal(ex,
                Map.of("status", 400, "error", "Validation failed", "fields", fields),
                headers, status, request);
    }

    /** Logs known business errors (404, 400, …) as warn, server errors as error. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        int code = ex.getStatusCode().value();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        if (code >= 500) {
            log.error("{} {} → {} {}", req.getMethod(), req.getRequestURI(), code, reason, ex);
        } else {
            log.warn("{} {} → {} {}", req.getMethod(), req.getRequestURI(), code, reason);
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("status", code, "error", reason));
    }

    /** Fallback for any unhandled exception — returns a generic 500 to avoid leaking internals. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "An unexpected error occurred"));
    }
}
