/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated
 * by story 6-operations (CAP-6): {@code OperationEntity},
 * {@code OperationAlertEntity}, their Spring Data interfaces, and
 * {@code JpaOperationsRepository}.
 */
package ru.mtsbank.cardoffice.operations.adapters.out.persistence;
