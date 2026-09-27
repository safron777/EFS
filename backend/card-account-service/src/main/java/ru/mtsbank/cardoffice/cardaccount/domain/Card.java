package ru.mtsbank.cardoffice.cardaccount.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single card row as returned to CAP-3 ({@code GET
 * /clients/{clientId}/products/{productId}/cards}). Framework-free by design
 * (spine Design Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code clientId}/{@code productId} are deliberately not fields here -- this
 * aggregate is one row of the collection response, not the query itself; the
 * client/product scoping lives only in the persistence query
 * ({@code CardRepository#findByClientIdAndProductId}). This also keeps the
 * class consistent with AD-2: a card may reference its owning client by id,
 * but never carries a copy of that client's other profile fields.
 *
 * {@code balanceMinorUnits} is a signed integer in kopecks (AD-3) -- the
 * mock's raw-ruble {@code balance} corrected per story Boundaries &
 * Constraints. {@code issuedAt}/{@code account}/{@code closedAt} are
 * nullable and their per-row presence is intentionally not uniform: an
 * active card carries {@code account}+{@code issuedAt} and no
 * {@code closedAt}; a closed card carries {@code closedAt} and no
 * {@code account}/{@code issuedAt} -- exactly mirroring {@code
 * mockData.cards}' per-row shape, reproduced (not padded with nulls) all the
 * way out to the web response.
 */
public final class Card {

    private final String id;
    private final String product;
    private final String masked;
    private final String subtitle;
    private final boolean isMain;
    private final String status;
    private final long balanceMinorUnits;
    private final LocalDate issuedAt;
    private final String account;
    private final LocalDate closedAt;

    public Card(
            String id,
            String product,
            String masked,
            String subtitle,
            boolean isMain,
            String status,
            long balanceMinorUnits,
            LocalDate issuedAt,
            String account,
            LocalDate closedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.product = Objects.requireNonNull(product, "product");
        this.masked = Objects.requireNonNull(masked, "masked");
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle");
        this.isMain = isMain;
        this.status = Objects.requireNonNull(status, "status");
        this.balanceMinorUnits = balanceMinorUnits;
        this.issuedAt = issuedAt;
        this.account = account;
        this.closedAt = closedAt;
    }

    public String getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public String getMasked() {
        return masked;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean isMain() {
        return isMain;
    }

    public String getStatus() {
        return status;
    }

    public long getBalanceMinorUnits() {
        return balanceMinorUnits;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public String getAccount() {
        return account;
    }

    public LocalDate getClosedAt() {
        return closedAt;
    }
}
