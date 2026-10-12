package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.time.LocalDate;

import ru.mtsbank.cardoffice.incidentcase.domain.Complaint;

/**
 * One element of the {@code GET /clients/{clientId}/complaints} response
 * array, and the full row returned by a successful {@code POST} (both
 * share this exact shape). Mirrors {@code mockData.complaints}'s fields
 * exactly (AD-8): {@code id, theme, created, term, status}, with {@code
 * created} as ISO-8601 ({@link LocalDate}) correcting the mock's {@code
 * dd.MM.yyyy} string (story Boundaries & Constraints).
 */
public record ComplaintResponse(String id, String theme, LocalDate created, String term, String status) {

    public static ComplaintResponse from(Complaint complaint) {
        return new ComplaintResponse(
                complaint.getId(), complaint.getTheme(), complaint.getCreated(), complaint.getTerm(),
                complaint.getStatus());
    }
}
