package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link NotificationMethodEntity}, used only by
 * {@link JpaNotificationsRepository}. The table is a singleton (one row,
 * fixed id {@value JpaNotificationsRepository#METHOD_ID}), so no custom
 * finder is needed beyond the inherited {@code findById}.
 */
public interface NotificationMethodJpaRepository extends JpaRepository<NotificationMethodEntity, String> {
}
