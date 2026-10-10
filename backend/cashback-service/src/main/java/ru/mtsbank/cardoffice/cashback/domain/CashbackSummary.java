package ru.mtsbank.cardoffice.cashback.domain;

import java.util.List;
import java.util.Objects;

/**
 * CAP-8 aggregate returned by {@code GET /cards/{cardId}/cashback}:
 * accumulated cashback + ordered category rates + the one active promo,
 * composed into a single response (story 8-cashback Intent). Framework-free
 * by design (spine Design Paradigm).
 *
 * {@code accumulatedMinorUnits} is a signed integer in kopecks (AD-3) --
 * the mock's raw-ruble {@code accumulated} corrected per story Boundaries
 * & Constraints. {@code categories} is always exactly the mock's four rows
 * in mock order ({@code cafe}, {@code azs}, {@code pharmacy}, {@code
 * other}) and {@code promo} is always present -- {@code
 * CashbackPage.jsx} dereferences {@code cashback.accumulated}/{@code
 * cashback.categories}/{@code cashback.promo.title} with zero null guards,
 * so this aggregate never allows a null promo or category list.
 */
public final class CashbackSummary {

    private final long accumulatedMinorUnits;
    private final List<CashbackCategory> categories;
    private final CashbackPromo promo;

    public CashbackSummary(long accumulatedMinorUnits, List<CashbackCategory> categories, CashbackPromo promo) {
        this.accumulatedMinorUnits = accumulatedMinorUnits;
        this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        this.promo = Objects.requireNonNull(promo, "promo");
    }

    public long getAccumulatedMinorUnits() {
        return accumulatedMinorUnits;
    }

    public List<CashbackCategory> getCategories() {
        return categories;
    }

    public CashbackPromo getPromo() {
        return promo;
    }
}
