package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link NotificationChannelEntity}, used only
 * by {@link JpaNotificationsRepository}. Ordered by {@code sort_order} so
 * the response array's row order is deterministic and matches {@code
 * mockData.notifications}' own order exactly (same precedent as {@code
 * CashbackCategoryJpaRepository}).
 */
public interface NotificationChannelJpaRepository extends JpaRepository<NotificationChannelEntity, String> {

    List<NotificationChannelEntity> findAllByOrderBySortOrderAsc();
}
