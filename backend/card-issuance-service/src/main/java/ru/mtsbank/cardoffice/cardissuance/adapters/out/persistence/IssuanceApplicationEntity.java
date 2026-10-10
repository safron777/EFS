package ru.mtsbank.cardoffice.cardissuance.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code issuance_applications} table -- schema:
 * db/changelog/changes/001-create-issuance-applications-and-seed.xml. Pure
 * persistence mapping; the domain object ({@code IssuanceApplication}) is
 * assembled from this by {@link JpaIssuanceRepository}, never used directly
 * outside this package.
 *
 * No {@code client_id}/{@code product_id} columns -- history is global in
 * this demo (story Boundaries & Constraints), same rationale as
 * card-account-service's {@code DocumentEntity}.
 *
 * {@code sortOrder} exists only to reproduce {@code mockData.issuance.history}'s
 * seeded array order exactly (app1 newest, app2/app3 older but not
 * descending -- not derivable by sorting on {@code date}), and to place new
 * rows created by {@code POST .../issuance/applications} after it, same
 * pattern as {@code DocumentEntity}.
 */
@Entity
@Table(name = "issuance_applications")
public class IssuanceApplicationEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "application_date", nullable = false)
    private LocalDate date;

    @Column(name = "card", nullable = false, length = 255)
    private String card;

    @Column(name = "method", nullable = false, length = 64)
    private String method;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected IssuanceApplicationEntity() {
        // JPA
    }

    public IssuanceApplicationEntity(String id, LocalDate date, String card, String method, String status, int sortOrder) {
        this.id = id;
        this.date = date;
        this.card = card;
        this.method = method;
        this.status = status;
        this.sortOrder = sortOrder;
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

    public int getSortOrder() {
        return sortOrder;
    }
}
