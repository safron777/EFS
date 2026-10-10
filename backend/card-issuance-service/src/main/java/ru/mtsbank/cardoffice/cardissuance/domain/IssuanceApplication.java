package ru.mtsbank.cardoffice.cardissuance.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single row of issuance history: either one of the three seeded rows
 * (story 5-card-issuance I/O & Edge-Case Matrix) or a new application created
 * by {@code POST .../issuance/applications}. Framework-free by design (spine
 * Design Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code clientId}/{@code productId} are deliberately not fields here, same
 * rationale as {@code Document} in card-account-service: history rows are a
 * global, unscoped list in this demo (story Boundaries & Constraints), not
 * per-client/per-product.
 */
public final class IssuanceApplication {

    private final String id;
    private final LocalDate date;
    private final String card;
    private final String method;
    private final String status;

    public IssuanceApplication(String id, LocalDate date, String card, String method, String status) {
        this.id = Objects.requireNonNull(id, "id");
        this.date = Objects.requireNonNull(date, "date");
        this.card = Objects.requireNonNull(card, "card");
        this.method = Objects.requireNonNull(method, "method");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCard() {
        return card;
    }

    public String getMethod() {
        return method;
    }

    public String getStatus() {
        return status;
    }
}
