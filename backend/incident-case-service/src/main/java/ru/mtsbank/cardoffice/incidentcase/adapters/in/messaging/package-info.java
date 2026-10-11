/**
 * Kafka consumers (AD-5, AD-11): the sole reader of {@code MassIncidentOpened} /
 * {@code MassIncidentUpdated} / {@code MassIncidentClosed} events. This service is
 * the system of record for mass-incident state; no other service reads it
 * directly from the monitoring system. Populated by story
 * 10-incident-case-mass-incidents (CAP-10): {@code MassIncidentEventEnvelope},
 * the three typed {@code data} records, {@code MassIncidentEventListener}
 * (one {@code @KafkaListener} method per topic, dead-letter routing on a
 * version mismatch), and {@code KafkaTopicsConfig}'s {@code NewTopic} beans.
 */
package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;
