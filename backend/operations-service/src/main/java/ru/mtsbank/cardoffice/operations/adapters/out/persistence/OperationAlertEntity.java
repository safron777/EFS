package ru.mtsbank.cardoffice.operations.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code operation_alerts} table -- schema:
 * db/changelog/changes/001-create-operations-schema.xml. One row per card
 * that currently has a failed-transfer alert ({@code card_id} is the
 * primary key, enforcing the 0-or-1-per-card cardinality); most cards have
 * no row at all. Pure persistence mapping; the domain object ({@code
 * FailedTransferAlert}) is assembled from this by {@link
 * JpaOperationsRepository}, never used directly outside this package.
 */
@Entity
@Table(name = "operation_alerts")
public class OperationAlertEntity {

    @Id
    @Column(name = "card_id", length = 64)
    private String cardId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    protected OperationAlertEntity() {
        // JPA
    }

    public String getCardId() {
        return cardId;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }
}
