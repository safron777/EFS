package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /incidents/mass/{incidentId}/attach},
 * mirroring {@code attachClientToIncident(clientId, incidentId)}'s {@code
 * {clientId}} body exactly (AD-8). {@code @NotBlank}, the same precedent as
 * {@code card-issuance-service}'s {@code CreateApplicationRequest} -- a
 * missing or blank {@code clientId} fails {@code @Valid} with a clean
 * {@code 400 VALIDATION_ERROR}.
 */
public record AttachClientRequest(@NotBlank String clientId) {
}
