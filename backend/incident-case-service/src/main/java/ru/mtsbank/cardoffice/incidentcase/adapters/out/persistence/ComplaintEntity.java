package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code complaints} table -- schema:
 * db/changelog/changes/002-create-single-incidents-and-complaints-schema.xml.
 * Pure persistence mapping; the domain object ({@code Complaint}) is
 * assembled from this by {@link JpaComplaintRepository}, never used
 * directly outside this package.
 *
 * {@code id} is this table's own business id (either a seeded literal mock
 * string such as {@code "Жалоба №118240"}, or a freshly generated {@code
 * complaint-<n>}), carried as the natural primary key -- same precedent as
 * {@code MassIncidentEntity}.
 */
@Entity
@Table(name = "complaints")
public class ComplaintEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "theme", nullable = false, length = 500)
    private String theme;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "term", nullable = false, length = 64)
    private String term;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    protected ComplaintEntity() {
        // JPA
    }

    public ComplaintEntity(
            String id, String clientId, String theme, LocalDate createdDate, String term, String status) {
        this.id = id;
        this.clientId = clientId;
        this.theme = theme;
        this.createdDate = createdDate;
        this.term = term;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTheme() {
        return theme;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public String getTerm() {
        return term;
    }

    public String getStatus() {
        return status;
    }
}
