package ru.mtsbank.cardoffice.notifications.domain;

/**
 * Thrown when the toggle mutation is called with a {@code notificationId}
 * that doesn't exist. Mapped to {@code 404} with the spine's error envelope
 * ({@code NOT_FOUND}) by the web adapter's exception handler -- this class
 * itself carries no HTTP concerns. Mirrors {@code card-account-service}'s
 * {@code DocumentNotFoundException} shape exactly (story Boundaries &
 * Constraints).
 */
public class NotificationChannelNotFoundException extends RuntimeException {

    private final String notificationId;

    public NotificationChannelNotFoundException(String notificationId) {
        super("Notification channel not found: " + notificationId);
        this.notificationId = notificationId;
    }

    public String getNotificationId() {
        return notificationId;
    }
}
