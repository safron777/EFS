package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed {@code data} shape for {@code incident-case.mass-incident.closed}
 * (story Boundaries & Constraints). See {@link MassIncidentOpenedData}'s
 * Javadoc for the {@code @JsonIgnoreProperties} rationale.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MassIncidentClosedData(String id, String status) {
}
