package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code cashback_summary} table -- schema:
 * db/changelog/changes/001-create-cashback-schema.xml. Pure persistence
 * mapping; the domain objects ({@code CashbackSummary}/{@code
 * CashbackPromo}) are assembled from this by {@link JpaCashbackRepository},
 * never used directly outside this package.
 *
 * Singleton table: exactly one row exists (seeded by
 * 002-seed-cashback.xml, id {@value JpaCashbackRepository#SUMMARY_ID}).
 * The promo is modeled as plain columns on this one row rather than a
 * separate table (story Design Notes) -- unlike story 7's arrest block
 * (0-or-1-per-card, genuinely optional), this mock's promo is always
 * present and singular, so a dedicated sub-table would be unneeded
 * ceremony for a fixture that never varies.
 */
@Entity
@Table(name = "cashback_summary")
public class CashbackSummaryEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "accumulated_minor_units", nullable = false)
    private long accumulatedMinorUnits;

    @Column(name = "promo_title", nullable = false, length = 255)
    private String promoTitle;

    @Column(name = "promo_text", nullable = false, length = 500)
    private String promoText;

    protected CashbackSummaryEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public long getAccumulatedMinorUnits() {
        return accumulatedMinorUnits;
    }

    public String getPromoTitle() {
        return promoTitle;
    }

    public String getPromoText() {
        return promoText;
    }
}
