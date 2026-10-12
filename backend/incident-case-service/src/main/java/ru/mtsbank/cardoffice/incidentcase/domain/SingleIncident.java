package ru.mtsbank.cardoffice.incidentcase.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CAP-11: a single row of {@code GET /clients/{clientId}/incidents/single}
 * -- either one of the two seeded rows (mirroring {@code
 * mockData.singleIncidents} verbatim) or a row created by {@code POST
 * /clients/{clientId}/incidents/single}. Framework-free by design (spine
 * Design Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code clientId} is deliberately not a field here -- same rationale as
 * {@code IssuanceApplication}: the client scoping is a persistence/query
 * concern ({@code findByClientId}), not part of the row's own shape on the
 * wire (story I/O Matrix: {@code [{id, theme, created, line, status}]}).
 *
 * {@code created} is ISO-8601 ({@link LocalDate}), correcting the mock's
 * {@code dd.MM.yyyy} string (story Boundaries & Constraints), the same
 * precedent as {@code IssuanceApplication.date}/{@code MassIncident.start}.
 */
public final class SingleIncident {

    private final String id;
    private final String theme;
    private final LocalDate created;
    private final String line;
    private final String status;

    public SingleIncident(String id, String theme, LocalDate created, String line, String status) {
        this.id = Objects.requireNonNull(id, "id");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.created = Objects.requireNonNull(created, "created");
        this.line = Objects.requireNonNull(line, "line");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getId() {
        return id;
    }

    public String getTheme() {
        return theme;
    }

    public LocalDate getCreated() {
        return created;
    }

    public String getLine() {
        return line;
    }

    public String getStatus() {
        return status;
    }
}
