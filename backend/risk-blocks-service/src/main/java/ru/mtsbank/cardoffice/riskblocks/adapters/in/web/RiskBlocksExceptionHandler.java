package ru.mtsbank.cardoffice.riskblocks.adapters.in.web;

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
 * Maps exceptions to the spine's error envelope. Extends {@link
 * ResponseEntityExceptionHandler} so Spring MVC's own framework exceptions
 * (405 Method Not Allowed, 415 Unsupported Media Type, a malformed
 * request, etc.) keep the status Spring already determined -- only the
 * response body is reshaped into the envelope.
 *
 * Built with this corrected shape from the start (story Boundaries &
 * Constraints) -- the already-final shape every service has carried since
 * story 6-operations, not re-deriving the intermediate, buggy form stories
 * 1-3 only reached after separate retroactive fixes (one of which dropped
 * the {@code Allow} header on a 405).
 *
 * This service has no domain-level not-found case and no {@code @Valid}
 * request body (CAP-7 is read-only, story Boundaries & Constraints), so
 * the {@link MethodArgumentNotValidException} branch below is never
 * actually exercised today -- it is kept anyway so the class matches the
 * final shape exactly, not an intermediate one missing this branch.
 */
@RestControllerAdvice
public class RiskBlocksExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String code;
        String message;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            // Jakarta Bean Validation failure -- build a clean "field: reason" message
            // from the BindingResult instead of letting exception.getMessage() leak the
            // full internal exception string (method signature, parameter index,
            // binding-result dump).
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
