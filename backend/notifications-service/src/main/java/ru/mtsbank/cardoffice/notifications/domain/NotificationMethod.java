package ru.mtsbank.cardoffice.notifications.domain;

import java.util.Objects;

/**
 * The client's primary contact method (ОМТ -- "Основной метод связи с
 * клиентом") returned alongside CAP-9's channel list. Framework-free by
 * design (spine Design Paradigm): no JPA/Spring annotations belong here.
 *
 * No method-change mutation exists for this story (story Boundaries &
 * Constraints) -- {@code NotificationsPage.jsx}'s "Изменить" button has no
 * {@code onClick} handler at all -- so this value object is read-only.
 */
public final class NotificationMethod {

    private final String title;
    private final String current;

    public NotificationMethod(String title, String current) {
        this.title = Objects.requireNonNull(title, "title");
        this.current = Objects.requireNonNull(current, "current");
    }

    public String getTitle() {
        return title;
    }

    public String getCurrent() {
        return current;
    }
}
