package ru.mtsbank.cardoffice.offers.adapters.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own
 * framework exceptions (405 Method Not Allowed, 415 Unsupported Media Type,
 * a malformed request, etc.) keep the status Spring already determined --
 * only the response body is reshaped into the spine's error envelope. A bare
 * {@code @ExceptionHandler(Exception.class)} has no visibility into that
 * status and would instead swallow every one of these into a misleading
 * {@code 500 INTERNAL_ERROR} (caught independently verifying this story: a
 * plain {@code POST} to this {@code GET}-only endpoint returned {@code 500}
 * instead of {@code 405} before this fix).
 */
@RestControllerAdvice
public class OfferExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String code;
        if (statusCode.is4xxClientError()) {
            code = "VALIDATION_ERROR";
        } else if (statusCode.is5xxServerError()) {
            code = "INTERNAL_ERROR";
        } else {
            code = "INTERNAL_ERROR";
        }
        String message = exception.getMessage() != null ? exception.getMessage() : "An unexpected error occurred.";
        return ResponseEntity.status(statusCode).body((Object) ErrorResponse.of(code, message));
    }

    /** Anything Spring MVC doesn't already recognize -- genuinely unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
