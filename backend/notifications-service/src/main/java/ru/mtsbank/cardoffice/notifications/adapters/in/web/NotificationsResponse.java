package ru.mtsbank.cardoffice.notifications.adapters.in.web;

import java.util.List;

import ru.mtsbank.cardoffice.notifications.domain.NotificationChannel;
import ru.mtsbank.cardoffice.notifications.domain.NotificationMethod;
import ru.mtsbank.cardoffice.notifications.domain.NotificationsSummary;

/**
 * {@code GET /cards/{cardId}/notifications} response body. Mirrors {@code
 * client.js}'s {@code getNotifications} assembly of {@code
 * mockData.notifications} + {@code mockData.notificationMethod} exactly
 * (AD-8): {@code items: [{id, title, subtitle, on, action}]}, {@code
 * method: {title, current}}.
 */
public record NotificationsResponse(List<ItemResponse> items, MethodResponse method) {

    public static NotificationsResponse from(NotificationsSummary summary) {
        return new NotificationsResponse(
                summary.getChannels().stream().map(ItemResponse::from).toList(),
                MethodResponse.from(summary.getMethod()));
    }

    public record ItemResponse(String id, String title, String subtitle, boolean on, String action) {

        public static ItemResponse from(NotificationChannel channel) {
            return new ItemResponse(
                    channel.getId(), channel.getTitle(), channel.getSubtitle(), channel.isOn(), channel.getAction());
        }
    }

    public record MethodResponse(String title, String current) {

        public static MethodResponse from(NotificationMethod method) {
            return new MethodResponse(method.getTitle(), method.getCurrent());
        }
    }
}
