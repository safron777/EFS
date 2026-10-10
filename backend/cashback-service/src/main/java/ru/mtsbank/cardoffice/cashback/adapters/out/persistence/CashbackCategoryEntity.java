package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code cashback_categories} table -- schema:
 * db/changelog/changes/001-create-cashback-schema.xml. Pure persistence
 * mapping; the domain object ({@code CashbackCategory}) is assembled from
 * this by {@link JpaCashbackRepository}, never used directly outside this
 * package.
 *
 * No {@code card_id}/{@code summary_id} column -- categories are global in
 * this demo (story Boundaries & Constraints: one demo-wide fixture, not
 * one row per card), the same simplification {@code DocumentEntity}
 * already made for account documents.
 *
 * {@code sortOrder} exists only to reproduce {@code
 * mockData.cashback.categories}' array order ({@code cafe}, {@code azs},
 * {@code pharmacy}, {@code other}) exactly -- that order isn't
 * alphabetical by {@code id} and isn't otherwise derivable from the row's
 * own fields, so an explicit ordering column is needed (same precedent as
 * {@code DocumentEntity#sortOrder}).
 */
@Entity
@Table(name = "cashback_categories")
public class CashbackCategoryEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "percent", nullable = false, length = 16)
    private String percent;

    @Column(name = "tone", nullable = false, length = 16)
    private String tone;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CashbackCategoryEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getPercent() {
        return percent;
    }

    public String getTone() {
        return tone;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
