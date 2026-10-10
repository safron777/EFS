package ru.mtsbank.cardoffice.cashback.domain;

import java.util.Objects;

/**
 * A single cashback category row as returned to CAP-8 ({@code GET
 * /cards/{cardId}/cashback}). Framework-free by design (spine Design
 * Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code percent}/{@code tone} deliberately stay the mock's exact literal
 * strings (e.g. {@code "5%"}, {@code "green"}) -- AD-3 binds money, not
 * percentages; {@code percent} is a display-only rate rendered directly by
 * {@code <Pill>} (story Boundaries & Constraints), the same category as
 * story 6's {@code status} or story 7's block {@code text}, not a value
 * needing a kopecks-style correction.
 *
 * {@code sortOrder} is deliberately not a field here -- it only drives the
 * persistence query's row order (see {@code CashbackCategoryEntity}'s
 * Javadoc), the same split {@code Document}/{@code DocumentEntity} already
 * established in card-account-service.
 */
public final class CashbackCategory {

    private final String id;
    private final String label;
    private final String percent;
    private final String tone;

    public CashbackCategory(String id, String label, String percent, String tone) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.percent = Objects.requireNonNull(percent, "percent");
        this.tone = Objects.requireNonNull(tone, "tone");
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
}
