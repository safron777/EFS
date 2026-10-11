package ru.mtsbank.cardoffice.notifications.adapters.in.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import ru.mtsbank.cardoffice.notifications.application.GetNotificationsUseCase;
import ru.mtsbank.cardoffice.notifications.application.ToggleNotificationUseCase;

/**
 * CAP-9 REST resource: {@code GET /cards/{cardId}/notifications} and {@code
 * POST /cards/{cardId}/notifications/{notificationId}} (AD-8), mirroring
 * {@code client.js}'s {@code getNotifications}/{@code toggleNotification}
 * exactly. Translates HTTP to the application layer only; no business logic
 * lives here (spine Design Paradigm).
 *
 * {@code cardId} is never validated against {@code card-account-service}
 * (AD-9) and never used to scope the toggle (story Boundaries &
 * Constraints) -- it is accepted as-is on both endpoints only to match the
 * URL shape, and the toggle path's {@code notificationId} alone identifies
 * the target row.
 */
@RestController
public class NotificationsController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final ToggleNotificationUseCase toggleNotificationUseCase;

    public NotificationsController(
            GetNotificationsUseCase getNotificationsUseCase, ToggleNotificationUseCase toggleNotificationUseCase) {
        this.getNotificationsUseCase = getNotificationsUseCase;
        this.toggleNotificationUseCase = toggleNotificationUseCase;
    }

    @GetMapping("/cards/{cardId}/notifications")
    public NotificationsResponse getNotifications(@PathVariable("cardId") String cardId) {
        return NotificationsResponse.from(getNotificationsUseCase.execute(cardId));
    }

    @PostMapping("/cards/{cardId}/notifications/{notificationId}")
    public OkResponse toggleNotification(
            // cardId is bound only to match the URL shape -- intentionally unused, never
            // validated or used to scope the target row (see class Javadoc, AD-9).
            @PathVariable("cardId") String cardId,
            @PathVariable("notificationId") String notificationId,
            @Valid @RequestBody ToggleNotificationRequest request) {
        toggleNotificationUseCase.execute(notificationId, request.on());
        return OkResponse.OK;
    }
}
