/**
 * Kafka consumers (AD-5, AD-11): the sole reader of {@code MassIncidentOpened} /
 * {@code MassIncidentUpdated} / {@code MassIncidentClosed} events. This service is
 * the system of record for mass-incident state; no other service reads it
 * directly from the monitoring system. Empty in Story 0 (infra scaffold only) --
 * the Kafka broker is stood up in docker-compose.yml but no consumer code exists
 * yet; that arrives with CAP-10's story.
 */
package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;
