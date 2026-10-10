/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated
 * by story 5-card-issuance (CAP-5): {@code IssuanceApplicationEntity},
 * {@code IssuanceApplicationJpaRepository} and {@code JpaIssuanceRepository}.
 */
package ru.mtsbank.cardoffice.cardissuance.adapters.out.persistence;
