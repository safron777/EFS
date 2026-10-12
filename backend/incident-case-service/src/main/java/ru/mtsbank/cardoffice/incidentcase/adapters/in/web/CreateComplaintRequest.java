package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /clients/{clientId}/complaints}, matching
 * exactly what {@code client.js}'s {@code createComplaint(clientId,
 * payload)} currently sends -- {@code {theme}} only (story Boundaries &
 * Constraints: the rest of the create form's required fields are still
 * undefined by product, per {@code README.md}'s open TODO; this story does
 * not invent them). A missing or blank value fails {@code @NotBlank},
 * mapped to {@code 400 VALIDATION_ERROR} by {@link
 * IncidentCaseExceptionHandler}. {@code @Size} bounds the {@code
 * complaints.theme varchar(500)} column with headroom to spare.
 */
public record CreateComplaintRequest(@NotBlank @Size(max = 255) String theme) {
}
