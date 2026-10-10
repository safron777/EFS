package ru.mtsbank.cardoffice.cardissuance.domain;

import java.util.Objects;

/**
 * The client's current in-flight issuance application, as shown by
 * {@code GET .../issuance}'s {@code currentApplication} field. Static --
 * not backed by a table (story Boundaries & Constraints: "same
 * simplification Story 2 made for offers, since nothing in the mock or any
 * button ever changes them") -- so {@link #fixture()} is the sole source,
 * hardcoded to match {@code mockData.issuance.currentApplication} exactly.
 * Never mutated by {@code POST .../issuance/applications} (story Boundaries
 * & Constraints: "No modification of currentApplication/newCard by the
 * POST").
 */
public final class CurrentApplication {

    private final String card;
    private final String status;
    private final String text;

    public CurrentApplication(String card, String status, String text) {
        this.card = Objects.requireNonNull(card, "card");
        this.status = Objects.requireNonNull(status, "status");
        this.text = Objects.requireNonNull(text, "text");
    }

    /** Hardcoded to match {@code mockData.issuance.currentApplication} exactly. */
    public static CurrentApplication fixture() {
        return new CurrentApplication("Zero", "Изготовлена", "Способ получения: курьер · ожидаемая дата 22.09.2026");
    }

    public String getCard() {
        return card;
    }

    public String getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }
}
