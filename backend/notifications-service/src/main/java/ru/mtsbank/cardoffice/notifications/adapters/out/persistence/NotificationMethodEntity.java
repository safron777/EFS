package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code notification_method} table -- schema:
 * db/changelog/changes/001-create-notifications-schema.xml. Pure persistence
 * mapping; the domain object ({@code NotificationMethod}) is assembled from
 * this by {@link JpaNotificationsRepository}, never used directly outside
 * this package.
 *
 * Singleton table: exactly one row exists (seeded by
 * 002-seed-notifications.xml, id {@value JpaNotificationsRepository#METHOD_ID}),
 * same precedent as {@code CashbackSummaryEntity}. No method-change mutation
 * exists for this story (story Boundaries & Constraints), so this entity has
 * no setters.
 *
 * The {@code current} column name matches the task list's own naming (CAP-9
 * Tasks & Acceptance: {@code notification_method (id PK singleton, title,
 * current)}) -- unlike {@code on}, {@code CURRENT} is a non-reserved keyword
 * in PostgreSQL (usable as an unquoted identifier), so no renaming footgun
 * applies here.
 */
@Entity
@Table(name = "notification_method")
public class NotificationMethodEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "current", nullable = false, length = 255)
    private String current;

    protected NotificationMethodEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCurrent() {
        return current;
    }
}
