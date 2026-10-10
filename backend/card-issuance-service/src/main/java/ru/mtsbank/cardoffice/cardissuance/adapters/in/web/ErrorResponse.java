package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

/**
 * Error envelope shape fixed by ARCHITECTURE-SPINE.md Consistency
 * Conventions: {@code { "error": { "code": string, "message": string,
 * "details"?: object } } }, uniform across all nine services. This is the
 * first story to actually trigger a real {@code VALIDATION_ERROR} from
 * Jakarta Bean Validation (story Boundaries & Constraints: "the first real
 * use of this code") rather than only the generic {@code 4xx}/{@code 5xx}
 * framework-exception mapping every prior story already exercised.
 */
public record ErrorResponse(ErrorBody error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }

    public record ErrorBody(String code, String message) {
    }
}
