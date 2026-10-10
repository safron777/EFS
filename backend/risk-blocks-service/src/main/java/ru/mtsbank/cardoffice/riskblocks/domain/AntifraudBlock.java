package ru.mtsbank.cardoffice.riskblocks.domain;

import java.util.Objects;

/**
 * The antifraud (АФ) block status for a card: title and display text.
 * Framework-free by design (spine Design Paradigm): no JPA/Spring
 * annotations belong here -- this service persists nothing for this
 * capability (story 7-risk-blocks Boundaries & Constraints).
 */
public final class AntifraudBlock {

    private final String title;
    private final String text;

    public AntifraudBlock(String title, String text) {
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
