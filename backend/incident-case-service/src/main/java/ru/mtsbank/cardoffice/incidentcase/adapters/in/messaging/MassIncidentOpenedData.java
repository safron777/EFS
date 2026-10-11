package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed {@code data} shape for {@code incident-case.mass-incident.opened}
 * (story Boundaries & Constraints): everything fixed at creation. {@code
 * @JsonIgnoreProperties(ignoreUnknown = true)} is AD-11's forward
 * compatibility rule -- an extra field the producer adds later must never
 * fail deserialization. {@code start} is parsed straight into {@link
 * LocalDate} because the wire contract is ISO-8601 (ARCHITECTURE-SPINE.md
 * Consistency Conventions), the same precedent as {@code operations-service}'s
 * {@code Operation.date}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MassIncidentOpenedData(
        String id, String theme, String product, LocalDate start, String status, String affected) {
}
