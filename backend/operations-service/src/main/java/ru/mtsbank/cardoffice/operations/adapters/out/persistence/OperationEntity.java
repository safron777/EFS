package ru.mtsbank.cardoffice.operations.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code operations} table -- schema:
 * db/changelog/changes/001-create-operations-schema.xml. Pure persistence
 * mapping; the domain object ({@code Operation}) is assembled from this by
 * {@link JpaOperationsRepository}, never used directly outside this
 * package.
 *
 * {@code cardId} scopes each row to its owning card (AD-2: reference by id
 * only, no cross-service copy) and backs the {@code
 * findByCardIdOrderByOpDateDesc} query.
 */
@Entity
@Table(name = "operations")
public class OperationEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "card_id", nullable = false, length = 64)
    private String cardId;

    @Column(name = "op_date", nullable = false)
    private LocalDate opDate;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    protected OperationEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public LocalDate getOpDate() {
        return opDate;
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
