/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated
 * by story 8-cashback (CAP-8): {@code CashbackSummaryEntity},
 * {@code CashbackCategoryEntity}, their Spring Data interfaces, and
 * {@code JpaCashbackRepository}.
 */
package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;
