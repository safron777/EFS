package ru.mtsbank.cardoffice.cardissuance.domain;

import java.util.List;
import java.util.Objects;

/**
 * CAP-5 aggregate returned by {@code GET .../issuance}: the static
 * {@link CurrentApplication}, the static {@link NewCardInfo}, and the real,
 * persisted, ordered {@code history} (story Boundaries & Constraints).
 * Framework-free by design (spine Design Paradigm).
 */
public final class Issuance {

    private final CurrentApplication currentApplication;
    private final NewCardInfo newCard;
    private final List<IssuanceApplication> history;

    public Issuance(CurrentApplication currentApplication, NewCardInfo newCard, List<IssuanceApplication> history) {
        this.currentApplication = Objects.requireNonNull(currentApplication, "currentApplication");
        this.newCard = Objects.requireNonNull(newCard, "newCard");
        this.history = List.copyOf(Objects.requireNonNull(history, "history"));
    }

    public CurrentApplication getCurrentApplication() {
        return currentApplication;
    }

    public NewCardInfo getNewCard() {
        return newCard;
    }

    public List<IssuanceApplication> getHistory() {
        return history;
    }
}
