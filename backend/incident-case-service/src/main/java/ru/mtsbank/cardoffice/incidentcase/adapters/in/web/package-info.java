/**
 * REST controllers implementing the resource shape mirrored from the frontend
 * contract (ARCHITECTURE-SPINE.md AD-8). Translates HTTP to application-layer
 * calls only -- no business logic. Populated by story 10-incident-case-mass-incidents
 * (CAP-10): {@code MassIncidentController}, its request/response DTOs, and
 * {@code IncidentCaseExceptionHandler}. Extended by story
 * 11-incident-case-single-incidents-complaints (CAP-11): {@code
 * SingleIncidentController}/{@code ComplaintController} and their
 * request/response DTOs, reusing {@code IncidentCaseExceptionHandler}
 * unchanged.
 */
package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;
