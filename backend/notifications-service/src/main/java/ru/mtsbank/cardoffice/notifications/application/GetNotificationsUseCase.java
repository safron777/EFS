package ru.mtsbank.cardoffice.notifications.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.notifications.domain.NotificationsRepository;
import ru.mtsbank.cardoffice.notifications.domain.NotificationsSummary;

/**
 * CAP-9: fetch the notifications summary for a card (story Intent). Pure
 * pass-through over the {@link NotificationsRepository} port -- {@code
 * cardId} is accepted for shape consistency only and never used to filter
 * (story Boundaries & Constraints: one demo-wide fixture, not one row per
 * card).
 */
@Service
public class GetNotificationsUseCase {

    private final NotificationsRepository notificationsRepository;

    public GetNotificationsUseCase(NotificationsRepository notificationsRepository) {
        this.notificationsRepository = notificationsRepository;
    }

    public NotificationsSummary execute(String cardId) {
        return notificationsRepository.getSummary(cardId);
    }
}
