package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.time.LocalDate;

import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncident;

/**
 * One element of the {@code GET /clients/{clientId}/incidents/single}
 * response array, and the full row returned by a successful {@code POST}
 * (both share this exact shape). Mirrors {@code
 * mockData.singleIncidents}'s fields exactly (AD-8): {@code id, theme,
 * created, line, status}, with {@code created} as ISO-8601 ({@link
 * LocalDate}) correcting the mock's {@code dd.MM.yyyy} string (story
 * Boundaries & Constraints).
 */
public record SingleIncidentResponse(String id, String theme, LocalDate created, String line, String status) {

    public static SingleIncidentResponse from(SingleIncident incident) {
        return new SingleIncidentResponse(
                incident.getId(), incident.getTheme(), incident.getCreated(), incident.getLine(),
                incident.getStatus());
    }
}
