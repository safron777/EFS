package ru.mtsbank.cardoffice.cashback.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services. CAP-8 has no
 * domain-level not-found case (story Boundaries & Constraints: any {@code
 * cardId} returns {@code 200}), so this service only ever emits the
 * shared vocabulary's {@code VALIDATION_ERROR} (e.g. a {@code 405} on a
 * non-GET request) and {@code INTERNAL_ERROR}.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
