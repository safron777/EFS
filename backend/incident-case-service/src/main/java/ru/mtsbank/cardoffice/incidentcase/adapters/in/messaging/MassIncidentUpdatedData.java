package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed {@code data} shape for {@code incident-case.mass-incident.updated}
 * (story Boundaries & Constraints): only the two fields that can
 * legitimately change after opening. See {@link MassIncidentOpenedData}'s
 * Javadoc for the {@code @JsonIgnoreProperties} rationale.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MassIncidentUpdatedData(String id, String status, String affected) {
}
