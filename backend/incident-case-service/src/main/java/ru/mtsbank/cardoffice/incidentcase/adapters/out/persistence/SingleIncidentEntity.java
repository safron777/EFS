package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code single_incidents} table -- schema:
 * db/changelog/changes/002-create-single-incidents-and-complaints-schema.xml.
 * Pure persistence mapping; the domain object ({@code SingleIncident}) is
 * assembled from this by {@link JpaSingleIncidentRepository}, never used
 * directly outside this package.
 *
 * {@code id} is this table's own business id (either a seeded literal mock
 * string such as {@code "ЕИ-118225"}, or a freshly generated {@code
 * ЕИ-<n>}), carried as the natural primary key -- same precedent as {@code
 * MassIncidentEntity}.
 */
@Entity
@Table(name = "single_incidents")
public class SingleIncidentEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "theme", nullable = false, length = 500)
    private String theme;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "line", nullable = false, length = 64)
    private String line;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    protected SingleIncidentEntity() {
        // JPA
    }

    public SingleIncidentEntity(
            String id, String clientId, String theme, LocalDate createdDate, String line, String status) {
        this.id = id;
        this.clientId = clientId;
        this.theme = theme;
        this.createdDate = createdDate;
        this.line = line;
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

    public String getLine() {
        return line;
    }

    public String getStatus() {
        return status;
    }
}
