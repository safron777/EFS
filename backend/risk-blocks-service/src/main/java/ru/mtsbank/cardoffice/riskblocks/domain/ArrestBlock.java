package ru.mtsbank.cardoffice.riskblocks.domain;

import java.util.Objects;

/**
 * The FSSP arrest block status for a card: title, display text, and the
 * seizure amount. Framework-free by design (spine Design Paradigm): no
 * JPA/Spring annotations belong here -- this service persists nothing for
 * this capability (story 7-risk-blocks Boundaries & Constraints).
 *
 * {@code amountMinorUnits} is the one wire-format correction this story
 * makes vs. the mock -- AD-3 explicitly binds "risk-blocks (arrest sum)":
 * the mock's {@code "сумма ареста 15 000 ₽"}, embedded only in {@code
 * text}'s prose, is pulled out into this signed-integer-kopecks field.
 * {@code text} itself stays byte-for-byte the mock's string -- the
 * frontend renders it as-is and this story makes no frontend change (story
 * Boundaries & Constraints).
 */
public final class ArrestBlock {

    private final String title;
    private final String text;
    private final long amountMinorUnits;

    public ArrestBlock(String title, String text, long amountMinorUnits) {
        this.title = Objects.requireNonNull(title, "title");
        this.text = Objects.requireNonNull(text, "text");
        this.amountMinorUnits = amountMinorUnits;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }
}
