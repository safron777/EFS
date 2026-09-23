package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services. This service
 * only ever emits {@code code: "NOT_FOUND"} (story 1-client-identity); the
 * rest of the shared vocabulary ({@code VALIDATION_ERROR},
 * {@code INTERNAL_ERROR}) is named there for later services to reuse.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
