package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code cards} table -- schema:
 * db/changelog/changes/001-create-card-account-schema.xml. Pure persistence
 * mapping; the domain object ({@code Card}) is assembled from this by
 * {@link JpaCardRepository}, never used directly outside this package.
 *
 * {@code clientId}/{@code productId} exist only here (and in the query),
 * not on the domain {@code Card} -- see {@code Card}'s class Javadoc.
 */
@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product", nullable = false)
    private String product;

    @Column(name = "masked", nullable = false)
    private String masked;

    @Column(name = "subtitle", nullable = false)
    private String subtitle;

    @Column(name = "is_main", nullable = false)
    private boolean isMain;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "balance_minor_units", nullable = false)
    private long balanceMinorUnits;

    @Column(name = "issued_at")
    private LocalDate issuedAt;

    @Column(name = "account")
    private String account;

    @Column(name = "closed_at")
    private LocalDate closedAt;

    protected CardEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getProductId() {
        return productId;
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
