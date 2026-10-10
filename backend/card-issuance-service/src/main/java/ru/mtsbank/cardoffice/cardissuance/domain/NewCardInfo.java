package ru.mtsbank.cardoffice.cardissuance.domain;

import java.util.Objects;

/**
 * The "issue a new card" informational blurb, as shown by {@code GET
 * .../issuance}'s {@code newCard} field. Static -- not backed by a table,
 * same rationale as {@link CurrentApplication} -- so {@link #fixture()} is
 * the sole source, hardcoded to match {@code mockData.issuance.newCard}
 * exactly. Never mutated by {@code POST .../issuance/applications}.
 */
public final class NewCardInfo {

    private final String text;

    public NewCardInfo(String text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    /** Hardcoded to match {@code mockData.issuance.newCard} exactly. */
    public static NewCardInfo fixture() {
        return new NewCardInfo("Виртуальная — мгновенно · пластиковая — 3–5 дней");
    }

    public String getText() {
        return text;
    }
}
