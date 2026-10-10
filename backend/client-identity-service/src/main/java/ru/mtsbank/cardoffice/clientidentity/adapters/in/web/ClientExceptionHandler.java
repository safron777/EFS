package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ru.mtsbank.cardoffice.clientidentity.domain.ClientNotFoundException;

/**
 * Maps domain exceptions to the spine's error envelope. Extends
 * {@link ResponseEntityExceptionHandler} so Spring MVC's own framework
 * exceptions (405 Method Not Allowed, 415 Unsupported Media Type, a
 * malformed request, etc.) keep the status Spring already determined --
 * only the response body is reshaped into the envelope. Fixed post-review:
 * the original bare {@code @ExceptionHandler(Exception.class)} had no
 * visibility into that status and swallowed every one of these into a
 * misleading {@code 500 INTERNAL_ERROR} (caught independently while
 * verifying story 2-offers, which copied this class's original shape: a
 * plain {@code POST} to this {@code GET}-only endpoint returned {@code 500}
 * instead of {@code 405}).
 */
@RestControllerAdvice
public class ClientExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(ClientNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", exception.getMessage()));
    }

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
        return ResponseEntity.status(statusCode).headers(headers).body((Object) ErrorResponse.of(code, message));
    }

    /**
     * Catch-all so any unmapped failure still leaves this service in the
     * spine's error envelope (Consistency Conventions), never Spring's
     * default {@code {"timestamp":...,"status":500,...}} body. Only reached
     * for exceptions Spring MVC doesn't already recognize -- genuinely
     * unexpected.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
