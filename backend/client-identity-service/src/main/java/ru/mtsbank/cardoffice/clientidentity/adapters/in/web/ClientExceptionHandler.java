package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ru.mtsbank.cardoffice.clientidentity.domain.ClientNotFoundException;

/**
 * Maps domain exceptions to the spine's error envelope. Story
 * 1-client-identity only emits {@code NOT_FOUND}; other services reuse the
 * same envelope shape and the rest of the shared code vocabulary as they add
 * their own handlers.
 */
@RestControllerAdvice
public class ClientExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", exception.getMessage()));
    }

    /**
     * Catch-all so any unmapped failure still leaves this service in the
     * spine's error envelope (Consistency Conventions), never Spring's
     * default {@code {"timestamp":...,"status":500,...}} body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
