/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated by
 * CAP-1 (story 1-client-identity): {@code ClientEntity}, {@code ClientTriggerEntity},
 * {@code ClientJpaRepository} and the {@code JpaClientRepository} port adapter.
 */
package ru.mtsbank.cardoffice.clientidentity.adapters.out.persistence;
