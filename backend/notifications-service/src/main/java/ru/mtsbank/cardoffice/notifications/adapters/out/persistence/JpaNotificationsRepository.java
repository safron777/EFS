package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.notifications.domain.NotificationChannel;
import ru.mtsbank.cardoffice.notifications.domain.NotificationChannelNotFoundException;
import ru.mtsbank.cardoffice.notifications.domain.NotificationMethod;
import ru.mtsbank.cardoffice.notifications.domain.NotificationsRepository;
import ru.mtsbank.cardoffice.notifications.domain.NotificationsSummary;

/**
 * Implements the domain {@link NotificationsRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link NotificationChannelEntity}/{@link
 * NotificationMethodEntity} (JPA) and {@link NotificationChannel}/{@link
 * NotificationMethod}/{@link NotificationsSummary} (domain).
 *
 * {@code cardId} on {@link #getSummary(String)} is accepted by the port's
 * method signature for shape consistency with sibling repositories only --
 * it is never used here to filter either table (story Boundaries &
 * Constraints: one demo-wide fixture).
 */
@Repository
public class JpaNotificationsRepository implements NotificationsRepository {

    /** The one {@code notification_method} row's fixed id, seeded by 002-seed-notifications.xml. */
    static final String METHOD_ID = "notification-method";

    private final NotificationChannelJpaRepository channelJpaRepository;
    private final NotificationMethodJpaRepository methodJpaRepository;

    public JpaNotificationsRepository(
            NotificationChannelJpaRepository channelJpaRepository,
            NotificationMethodJpaRepository methodJpaRepository) {
        this.channelJpaRepository = channelJpaRepository;
        this.methodJpaRepository = methodJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationsSummary getSummary(String cardId) {
        List<NotificationChannel> channels = channelJpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(JpaNotificationsRepository::toDomainChannel)
                .toList();

        NotificationMethodEntity methodEntity = methodJpaRepository
                .findById(METHOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "notification_method singleton row (" + METHOD_ID + ") is missing -- seed data not applied"));

        NotificationMethod method = new NotificationMethod(methodEntity.getTitle(), methodEntity.getCurrent());

        return new NotificationsSummary(channels, method);
    }

    @Override
    @Transactional
    public void setChannelState(String notificationId, boolean on) {
        NotificationChannelEntity entity = channelJpaRepository
                .findById(notificationId)
                .orElseThrow(() -> new NotificationChannelNotFoundException(notificationId));
        entity.setEnabled(on);
        channelJpaRepository.save(entity);
    }

    private static NotificationChannel toDomainChannel(NotificationChannelEntity entity) {
        return new NotificationChannel(
                entity.getId(), entity.getTitle(), entity.getSubtitle(), entity.isEnabled(), entity.getAction());
    }
}
