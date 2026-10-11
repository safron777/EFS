package ru.mtsbank.cardoffice.notifications.domain;

import java.util.List;
import java.util.Objects;

/**
 * CAP-9 aggregate returned by {@code GET /cards/{cardId}/notifications}:
 * the ordered channel list plus the one notification method, composed into
 * a single response (story Intent), mirroring what {@code client.js}'s
 * {@code getNotifications} assembles from {@code mockData.notifications} +
 * {@code mockData.notificationMethod} (AD-8). Framework-free by design
 * (spine Design Paradigm).
 *
 * {@code channels} is always exactly the mock's three rows in mock order and
 * {@code method} is always present -- {@code NotificationsPage.jsx}
 * dereferences {@code data.items.map}/{@code data.method.title} with zero
 * null guards, so this aggregate never allows a null method or channel list.
 */
public final class NotificationsSummary {

    private final List<NotificationChannel> channels;
    private final NotificationMethod method;

    public NotificationsSummary(List<NotificationChannel> channels, NotificationMethod method) {
        this.channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        this.method = Objects.requireNonNull(method, "method");
    }

    public List<NotificationChannel> getChannels() {
        return channels;
    }

    public NotificationMethod getMethod() {
        return method;
    }
}
