package ru.mtsbank.cardoffice.operations.domain;

import java.util.Objects;

/**
 * The 0-or-1-per-card failed-SBP-transfer alert shown above a card's
 * operation list. Modeled as its own value object (and its own table,
 * {@code operation_alerts}) rather than a nullable field on every row --
 * an alert is a sub-aggregate of the card's operations history, not a
 * per-row fact (story Tasks & Acceptance rationale). Framework-free by
 * design (spine Design Paradigm).
 */
public final class FailedTransferAlert {

    private final String title;
    private final String text;

    public FailedTransferAlert(String title, String text) {
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
