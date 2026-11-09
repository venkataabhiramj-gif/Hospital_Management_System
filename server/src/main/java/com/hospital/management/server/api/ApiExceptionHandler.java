package com.hospital.management.server.api;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiModels.ApiError> responseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return error(status, exception.getReason() == null ? status.getReasonPhrase() : exception.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiModels.ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiModels.ApiError> unreadableRequest(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request contains an invalid value or date format");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiModels.ApiError> duplicate(DuplicateKeyException exception) {
        return error(HttpStatus.CONFLICT, "A record with those details already exists");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiModels.ApiError> integrityConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "The change conflicts with a record or business rule");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiModels.ApiError> illegalArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiModels.ApiError> accessDenied(AccessDeniedException exception) {
        return error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    private ResponseEntity<ApiModels.ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiModels.ApiError(
                OffsetDateTime.now(ZoneOffset.UTC), status.value(), message));
    }
}
