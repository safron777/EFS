package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST .../issuance/applications} -- a new endpoint
 * invented for this story, with no existing {@code client.js} call to mirror
 * (story Design Notes). Both fields are required (story Boundaries &
 * Constraints: "A missing/blank card or method ... returns 400"); a missing
 * or blank value fails {@code @NotBlank}, which {@link IssuanceExceptionHandler}
 * maps to {@code 400 VALIDATION_ERROR}. {@code @Size} bounds mirror the
 * {@code issuance_applications} table's {@code card varchar(255)}/
 * {@code method varchar(64)} columns, so an over-length value 400s here
 * instead of failing at JPA flush time.
 */
public record CreateApplicationRequest(
        @NotBlank @Size(max = 255) String card, @NotBlank @Size(max = 64) String method) {
}
