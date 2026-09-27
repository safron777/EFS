package ru.mtsbank.cardoffice.offers.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services. This service
 * only ever emits {@code code: "INTERNAL_ERROR"} (no not-found case exists
 * for CAP-2 -- story 2-offers Code Map); the rest of the shared vocabulary is
 * named in the spine for other services to reuse.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
