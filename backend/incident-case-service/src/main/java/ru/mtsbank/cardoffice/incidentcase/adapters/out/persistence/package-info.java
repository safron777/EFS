/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated by
 * story 10-incident-case-mass-incidents (CAP-10): {@code MassIncidentEntity}/{@code
 * MassIncidentAttachmentEntity}, their Spring Data interfaces, and {@code
 * JpaMassIncidentRepository}.
 */
package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;
