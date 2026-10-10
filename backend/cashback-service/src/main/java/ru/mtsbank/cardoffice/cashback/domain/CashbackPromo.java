package ru.mtsbank.cardoffice.cashback.domain;

import java.util.Objects;

/**
 * The one active promo attached to CAP-8's cashback summary. Framework-free
 * by design (spine Design Paradigm): no JPA/Spring annotations belong here.
 *
 * Modeled as a value object nested inside {@link CashbackSummary} rather
 * than its own repository-backed aggregate: unlike story 7's arrest block
 * (0-or-1-per-card, genuinely optional), this mock's promo is always
 * present and singular for the one demo-wide fixture (story Design Notes),
 * so a dedicated sub-table/aggregate would be unneeded ceremony.
 */
public final class CashbackPromo {

    private final String title;
    private final String text;

    public CashbackPromo(String title, String text) {
        this.title = Objects.requireNonNull(title, "title");
        this.text = Objects.requireNonNull(text, "text");
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}
