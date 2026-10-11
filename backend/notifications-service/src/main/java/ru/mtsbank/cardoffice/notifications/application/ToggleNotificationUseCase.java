package ru.mtsbank.cardoffice.notifications.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.notifications.domain.NotificationChannelNotFoundException;
import ru.mtsbank.cardoffice.notifications.domain.NotificationsRepository;

/**
 * CAP-9: the toggle mutation. This backend's first true update-in-place
 * state change (story Intent/Design Notes) -- every prior mutation (story
 * 4's document send/print, story 5's application creation) only ever
 * triggered a side effect or inserted a new row; this one flips an existing
 * row's field and the next {@code GET} must observe it.
 *
 * {@code cardId} is never accepted here (story Boundaries & Constraints:
 * toggling never validates or scopes by {@code cardId} -- only {@code
 * notificationId} identifies the target row).
 */
@Service
public class ToggleNotificationUseCase {

    private final NotificationsRepository notificationsRepository;

    public ToggleNotificationUseCase(NotificationsRepository notificationsRepository) {
        this.notificationsRepository = notificationsRepository;
    }

    /**
     * @throws NotificationChannelNotFoundException if no channel with that
     *     id exists
     */
    public void execute(String notificationId, boolean on) {
        notificationsRepository.setChannelState(notificationId, on);
    }
}
