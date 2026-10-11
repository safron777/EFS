package ru.mtsbank.cardoffice.notifications.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code notification_channels} table -- schema:
 * db/changelog/changes/001-create-notifications-schema.xml. Pure persistence
 * mapping; the domain object ({@code NotificationChannel}) is assembled from
 * this by {@link JpaNotificationsRepository}, never used directly outside
 * this package.
 *
 * No {@code card_id} column -- channels are global in this demo (story
 * Boundaries & Constraints: one demo-wide fixture, not one row per card),
 * the same simplification {@code CashbackCategoryEntity} already made.
 *
 * The persisted boolean column is named {@code enabled}, not {@code on} --
 * {@code ON} is a SQL keyword Postgres reserves for {@code JOIN ... ON};
 * avoiding it as a column name avoids having to quote it everywhere. The
 * domain/wire field stays {@code on}/{@code isOn()} to mirror the mock
 * exactly (story Boundaries & Constraints).
 *
 * {@code sortOrder} exists only to reproduce {@code
 * mockData.notifications}' array order ({@code sms-ops}, {@code sms-plan},
 * {@code push}) exactly -- the same precedent as {@code
 * CashbackCategoryEntity#sortOrder}.
 */
@Entity
@Table(name = "notification_channels")
public class NotificationChannelEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", nullable = false, length = 500)
    private String subtitle;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected NotificationChannelEntity() {
        // JPA
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAction() {
        return action;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
