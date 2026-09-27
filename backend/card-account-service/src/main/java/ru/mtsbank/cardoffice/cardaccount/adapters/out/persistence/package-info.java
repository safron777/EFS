/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated
 * by story 3-card-account-cards (CAP-3): {@code CardEntity},
 * {@code CardJpaRepository} and {@code JpaCardRepository}.
 */
package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;
