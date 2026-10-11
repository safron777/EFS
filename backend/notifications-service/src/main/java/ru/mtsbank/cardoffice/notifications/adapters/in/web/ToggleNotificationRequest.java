package ru.mtsbank.cardoffice.notifications.adapters.in.web;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /cards/{cardId}/notifications/{notificationId}},
 * mirroring {@code toggleNotification(cardId, notificationId, on)}'s {@code
 * {on: boolean}} body exactly (AD-8). {@code @NotNull} so a missing {@code
 * on} field fails {@code @Valid} with a clean {@code 400 VALIDATION_ERROR}
 * (I/O & Edge-Case Matrix) instead of silently defaulting to {@code false}.
 */
public record ToggleNotificationRequest(@NotNull Boolean on) {
}
