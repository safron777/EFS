package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentNotFoundException;

/**
 * Maps exceptions to the spine's error envelope. Extends {@link
 * ResponseEntityExceptionHandler} so Spring MVC's own framework exceptions
 * (405 Method Not Allowed, 415 Unsupported Media Type, a malformed request,
 * {@code MethodArgumentNotValidException} on a failed {@code @Valid}, etc.)
 * keep the status Spring already determined -- only the response body is
 * reshaped into the envelope.
 *
 * This service ships in this already-final shape from the start (story
 * Boundaries & Constraints), the same shape {@code notifications-service}
 * (story 9) carries, extended with exactly one addition: the {@link
 * MassIncidentNotFoundException} mapping below, mirroring {@code
 * NotificationChannelNotFoundException}'s shape exactly -- one advice
 * class, no second one.
 */
@RestControllerAdvice
public class IncidentCaseExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MassIncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMassIncidentNotFound(MassIncidentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", exception.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String code;
        String message;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            // Jakarta Bean Validation failure on AttachClientRequest -- build a clean
            // "field: reason" message from the BindingResult instead of letting
            // exception.getMessage() leak the full internal exception string.
            code = "VALIDATION_ERROR";
            message = validationException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else if (statusCode.is4xxClientError()) {
            code = "VALIDATION_ERROR";
            message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
        } else {
            code = "INTERNAL_ERROR";
            message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
        }
        return ResponseEntity.status(statusCode).headers(headers).body((Object) ErrorResponse.of(code, message));
    }

    /** Anything Spring MVC doesn't already recognize -- genuinely unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
