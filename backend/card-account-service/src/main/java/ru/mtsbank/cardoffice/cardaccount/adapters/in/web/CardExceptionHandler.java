package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ru.mtsbank.cardoffice.cardaccount.domain.DocumentNotFoundException;

/**
 * Maps exceptions to the spine's error envelope. Extends
 * {@link ResponseEntityExceptionHandler} so Spring MVC's own framework
 * exceptions (405 Method Not Allowed, 415 Unsupported Media Type, a
 * malformed request, etc.) keep the status Spring already determined --
 * only the response body is reshaped into the envelope.
 *
 * This service is built with this corrected shape from the start (story
 * Boundaries & Constraints), copying what stories 1-client-identity and
 * 2-offers each shipped as a bare {@code @ExceptionHandler(Exception.class)}
 * and had to patch after independent verification caught it silently
 * converting Spring's own {@code 405}/{@code 415} into a misleading
 * {@code 500} -- not re-deriving that original, buggy form.
 *
 * Story 4-card-account-documents adds the {@link DocumentNotFoundException}
 * mapping below, reusing this existing corrected advice class rather than
 * creating a second one (story Boundaries & Constraints).
 */
@RestControllerAdvice
public class CardExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException exception) {
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

    /** Anything Spring MVC doesn't already recognize -- genuinely unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
