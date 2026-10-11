package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.time.LocalDate;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncident;

/**
 * One element of the {@code GET /incidents/mass} response array. Mirrors
 * {@code mockData.massIncidents}' shape exactly (AD-8): {@code id, theme,
 * product, start, status, affected, matchesClient}.
 *
 * {@code matchesClient} is unconditionally {@code false} (story Boundaries
 * & Constraints) -- it is not a {@link MassIncident} field at all, only a
 * response-layer constant this DTO supplies on every row.
 */
public record MassIncidentResponse(
        String id, String theme, String product, LocalDate start, String status, String affected,
        boolean matchesClient) {

    public static MassIncidentResponse from(MassIncident incident) {
        return new MassIncidentResponse(
                incident.getId(),
                incident.getTheme(),
                incident.getProduct(),
                incident.getStart(),
                incident.getStatus(),
                incident.getAffected(),
                false);
    }
}
