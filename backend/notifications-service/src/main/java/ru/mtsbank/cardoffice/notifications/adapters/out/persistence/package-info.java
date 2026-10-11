/**
 * JPA repositories implementing domain repository ports against this service's
 * own PostgreSQL schema (AD-1: DB-per-service, no shared schema). Populated by
 * story 9-notifications (CAP-9): {@code NotificationChannelEntity}/{@code
 * NotificationMethodEntity}, their Spring Data interfaces, and
 * {@code JpaNotificationsRepository}.
 */
package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;
