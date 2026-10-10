package ru.mtsbank.cardoffice.riskblocks.domain;

import java.util.Objects;

/**
 * The SFM ("Списки" service-control check) block status for a card: title
 * and display text. Framework-free by design (spine Design Paradigm): no
 * JPA/Spring annotations belong here -- this service persists nothing for
 * this capability (story 7-risk-blocks Boundaries & Constraints).
 *
 * {@code text}'s own embedded amount ({@code "проверка операции на 5 000
 * ₽"}) is deliberately NOT extracted into a structured field -- AD-3's
 * binding names "risk-blocks (arrest sum)" specifically; this figure
 * describes which transaction triggered the check, not a block-level
 * monetary fact like the arrest's seizure amount (story Boundaries &
 * Constraints).
 */
public final class SfmBlock {

    private final String title;
    private final String text;

    public SfmBlock(String title, String text) {
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
