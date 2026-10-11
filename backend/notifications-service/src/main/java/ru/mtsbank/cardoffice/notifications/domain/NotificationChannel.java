package ru.mtsbank.cardoffice.notifications.domain;

import java.util.Objects;

/**
 * A single notification-channel row as returned to CAP-9 ({@code GET
 * /cards/{cardId}/notifications}) and flipped by its toggle mutation
 * ({@code POST /cards/{cardId}/notifications/{notificationId}}). Framework-free
 * by design (spine Design Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code on}/{@code isOn()} deliberately mirror the mock's wire field name
 * exactly (story Boundaries & Constraints) even though the persisted column
 * backing it is named {@code enabled} -- {@code on} is a SQL keyword reserved
 * for {@code JOIN ... ON}, so the persistence layer avoids it while this
 * domain/wire-facing accessor keeps the mock's own name.
 *
 * {@code action} stays the mock's literal display string (e.g. {@code
 * "Отключить"}, {@code "Настроить"}) unchanged -- it carries no backend
 * meaning ({@code NotificationsPage.jsx}'s secondary button only displays it
 * with no {@code onClick} handler at all).
 */
public final class NotificationChannel {

    private final String id;
    private final String title;
    private final String subtitle;
    private final boolean on;
    private final String action;

    public NotificationChannel(String id, String title, String subtitle, boolean on, String action) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle");
        this.on = on;
        this.action = Objects.requireNonNull(action, "action");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean isOn() {
        return on;
    }

    public String getAction() {
        return action;
    }
}
