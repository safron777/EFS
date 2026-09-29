package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services. CAP-3's
 * card-list endpoint has no domain-level not-found case (story
 * 3-card-account-cards Boundaries & Constraints), so it only ever emitted
 * the shared vocabulary's {@code VALIDATION_ERROR} (e.g. a {@code 405} on a
 * non-GET request) and {@code INTERNAL_ERROR}; story 4-card-account-documents
 * adds a real {@code NOT_FOUND} case for an unknown {@code documentId} on
 * {@code send}/{@code print}.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
