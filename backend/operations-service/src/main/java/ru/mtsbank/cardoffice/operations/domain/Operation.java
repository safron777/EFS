package ru.mtsbank.cardoffice.operations.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single operation-history row as returned to CAP-6 ({@code GET
 * /cards/{cardId}/operations}). Framework-free by design (spine Design
 * Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code amountMinorUnits} is a signed integer in kopecks (AD-3) -- the
 * mock's formatted {@code "−5 000 ₽"}/{@code "+450 ₽"} strings corrected per
 * story Boundaries & Constraints. {@code date} is ISO-8601 ({@link
 * LocalDate}), the mock's year-less {@code "12.09"} pinned to 2026 by
 * {@code failedAlert.text}'s embedded {@code "12.09.2026"} (same story
 * section). {@code status} is kept as the mock's exact literal Russian
 * string (deliberately *not* normalized to an enum code) because {@code
 * src/components/ui/Pill.jsx}'s {@code STATUS_TONE} map keys off it exactly
 * -- the one field in this service that stays a raw frontend string all the
 * way through.
 */
public final class Operation {

    private final String id;
    private final LocalDate date;
    private final String description;
    private final long amountMinorUnits;
    private final String status;

    public Operation(String id, LocalDate date, String description, long amountMinorUnits, String status) {
        this.id = Objects.requireNonNull(id, "id");
        this.date = Objects.requireNonNull(date, "date");
        this.description = Objects.requireNonNull(description, "description");
        this.amountMinorUnits = amountMinorUnits;
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public long getAmountMinorUnits() {
        return amountMinorUnits;
    }

    public String getStatus() {
        return status;
    }
}
