package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

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

/**
 * Maps exceptions to the spine's error envelope. Extends
 * {@link ResponseEntityExceptionHandler} so Spring MVC's own framework
 * exceptions -- including {@code MethodArgumentNotValidException}, thrown
 * when {@code @Valid} fails on {@link CreateApplicationRequest} -- keep the
 * status Spring already determined (here, {@code 400}) while only the
 * response body is reshaped into the envelope. Built with this corrected
 * shape from the start (story Boundaries & Constraints), per stories 1-3's
 * lesson that a bare {@code @ExceptionHandler(Exception.class)} silently
 * turns Spring's own {@code 405}/{@code 415}/{@code 400} into a misleading
 * {@code 500}.
 *
 * This service has no domain-level not-found case (story Boundaries &
 * Constraints lists no 404 scenario for CAP-5), so unlike
 * {@code CardExceptionHandler} in card-account-service, no extra
 * {@code @ExceptionHandler} for a domain exception is needed here.
 */
@RestControllerAdvice
public class IssuanceExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String code;
        String message;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            // Jakarta Bean Validation failure on CreateApplicationRequest -- build a clean
            // "field: reason" message from the BindingResult instead of letting
            // exception.getMessage() leak the full internal exception string (method
            // signature, parameter index, binding-result dump).
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
