package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.JsonNode;

/**
 * Generic Kafka envelope shared by all three {@code
 * incident-case.mass-incident.*} listeners (AD-11): {@code {eventId,
 * eventType, occurredAt, version, data}}. {@code data} is kept as a raw
 * {@link JsonNode} here -- each listener converts it to the typed record
 * matching its own topic ({@link MassIncidentOpenedData}/{@link
 * MassIncidentUpdatedData}/{@link MassIncidentClosedData}) only after the
 * {@code version} check passes.
 *
 * {@code occurredAt} stays a plain {@link String}: nothing in this service
 * computes with it (it exists for traceability/logging only), so parsing it
 * into a temporal type would add risk for no behavioral gain.
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} applies forward
 * compatibility at the envelope level too, on top of AD-11's explicit
 * requirement for {@code data} (Boundaries & Constraints) -- a defensive
 * extension, not a contract gap, since the envelope's own five fields are
 * fixed by AD-11.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MassIncidentEventEnvelope(String eventId, String eventType, String occurredAt, int version, JsonNode data) {
}
