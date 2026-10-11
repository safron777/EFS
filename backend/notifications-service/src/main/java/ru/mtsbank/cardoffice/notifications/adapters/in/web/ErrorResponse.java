package ru.mtsbank.cardoffice.notifications.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
