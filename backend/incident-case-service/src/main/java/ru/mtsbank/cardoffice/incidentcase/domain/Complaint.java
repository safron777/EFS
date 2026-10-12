package ru.mtsbank.cardoffice.incidentcase.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CAP-11: a single row of {@code GET /clients/{clientId}/complaints} --
 * either one of the two seeded rows (mirroring {@code mockData.complaints}
 * verbatim) or a row created by {@code POST
 * /clients/{clientId}/complaints}. Framework-free by design (spine Design
 * Paradigm): no JPA/Spring annotations belong here.
 *
 * {@code clientId} is deliberately not a field here, same rationale as
 * {@link SingleIncident}. {@code created} is ISO-8601 ({@link LocalDate}),
 * correcting the mock's {@code dd.MM.yyyy} string (story Boundaries &
 * Constraints).
 */
public final class Complaint {

    private final String id;
    private final String theme;
    private final LocalDate created;
    private final String term;
    private final String status;

    public Complaint(String id, String theme, LocalDate created, String term, String status) {
        this.id = Objects.requireNonNull(id, "id");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.created = Objects.requireNonNull(created, "created");
        this.term = Objects.requireNonNull(term, "term");
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

    public String getTerm() {
        return term;
    }

    public String getStatus() {
        return status;
    }
}
