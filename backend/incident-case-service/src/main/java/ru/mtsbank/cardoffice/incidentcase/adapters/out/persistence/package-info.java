/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated by
 * story 10-incident-case-mass-incidents (CAP-10): {@code MassIncidentEntity}/{@code
 * MassIncidentAttachmentEntity}, their Spring Data interfaces, and {@code
 * JpaMassIncidentRepository}. Extended by story
 * 11-incident-case-single-incidents-complaints (CAP-11): {@code
 * SingleIncidentEntity}/{@code ComplaintEntity}, their Spring Data
 * interfaces, and {@code JpaSingleIncidentRepository}/{@code
 * JpaComplaintRepository}.
 */
package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;
