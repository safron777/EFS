package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code mass_incidents} table -- schema:
 * db/changelog/changes/001-create-incident-case-schema.xml. Pure persistence
 * mapping; the domain object ({@code MassIncident}) is assembled from this
 * by {@link JpaMassIncidentRepository}, never used directly outside this
 * package.
 *
 * {@code id} is the monitoring system's own mass-incident id (e.g. {@code
 * "MI-4471"}), carried verbatim from the Kafka event's {@code data.id} --
 * this service never generates it.
 */
@Entity
@Table(name = "mass_incidents")
public class MassIncidentEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "theme", nullable = false, length = 500)
    private String theme;

    @Column(name = "product", nullable = false, length = 255)
    private String product;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "affected", nullable = false, length = 255)
    private String affected;

    protected MassIncidentEntity() {
        // JPA
    }

    public MassIncidentEntity(
            String id, String theme, String product, LocalDate startDate, String status, String affected) {
        this.id = id;
        this.theme = theme;
        this.product = product;
        this.startDate = startDate;
        this.status = status;
        this.affected = affected;
    }

    public String getId() {
        return id;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAffected() {
        return affected;
    }

    public void setAffected(String affected) {
        this.affected = affected;
    }
}
