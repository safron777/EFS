/**
 * Use cases / application services orchestrating domain objects through ports.
 * The only layer allowed to coordinate more than one domain object at a time.
 * Populated by story 10-incident-case-mass-incidents (CAP-10): {@code
 * GetMassIncidentsUseCase}, {@code AttachClientUseCase}, and {@code
 * ApplyMassIncidentEventUseCase} (the one use case all three Kafka
 * listeners call). Extended by story
 * 11-incident-case-single-incidents-complaints (CAP-11): {@code
 * GetSingleIncidentsUseCase}, {@code CreateSingleIncidentUseCase}, {@code
 * GetComplaintsUseCase}, and {@code CreateComplaintUseCase}.
 */
package ru.mtsbank.cardoffice.incidentcase.application;
