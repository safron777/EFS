package ru.mtsbank.cardoffice.offers.domain;

import java.util.Objects;

/**
 * The client's top bank offer: title, body text, and a count of additional
 * offers not shown here. Framework-free by design (spine Design Paradigm):
 * no JPA/Spring annotations belong here -- this service persists nothing for
 * this capability (story 2-offers Boundaries & Constraints).
 */
public final class Offer {

    private final String title;
    private final String text;
    private final int moreCount;

    public Offer(String title, String text, int moreCount) {
        this.title = Objects.requireNonNull(title, "title");
        this.text = Objects.requireNonNull(text, "text");
        if (moreCount < 0) {
            throw new IllegalArgumentException("moreCount must not be negative, got: " + moreCount);
        }
        this.moreCount = moreCount;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public int getMoreCount() {
        return moreCount;
    }
}
