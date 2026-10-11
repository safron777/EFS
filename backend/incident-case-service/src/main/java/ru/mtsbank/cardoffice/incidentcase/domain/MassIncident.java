package ru.mtsbank.cardoffice.incidentcase.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A single mass incident as returned to CAP-10 ({@code GET
 * /incidents/mass}). Framework-free by design (spine Design Paradigm): no
 * JPA/Spring annotations belong here.
 *
 * This is the read model populated exclusively by {@code
 * MassIncidentEventListener}'s three Kafka consumers (AD-5) -- {@code
 * theme}/{@code product}/{@code start} are fixed at {@code opened} time and
 * never change; {@code status}/{@code affected} can be revised by a later
 * {@code updated} event, and {@code status} again by a {@code closed} event
 * (story Boundaries & Constraints).
 *
 * {@code matchesClient} is deliberately NOT a field here -- it is always
 * {@code false} in every response (story Boundaries & Constraints: no
 * Kafka event carries it, and computing a real value would require a
 * synchronous call into {@code operations-service}, forbidden by AD-9). The
 * web adapter's response DTO supplies that constant on its own.
 *
 * {@code start} is ISO-8601 ({@link LocalDate}), correcting the mock's
 * {@code dd.MM.yyyy} string (story Boundaries & Constraints), the same
 * precedent as {@code operations-service}'s {@code Operation.date}. {@code
 * affected} stays the mock's free-text approximate string (e.g. {@code "~3
 * 200 клиентов"}) unchanged -- a description, not a number, so AD-3 doesn't
 * apply to it.
 */
public final class MassIncident {

    private final String id;
    private final String theme;
    private final String product;
    private final LocalDate start;
    private final String status;
    private final String affected;

    public MassIncident(String id, String theme, String product, LocalDate start, String status, String affected) {
        this.id = Objects.requireNonNull(id, "id");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.product = Objects.requireNonNull(product, "product");
        this.start = Objects.requireNonNull(start, "start");
        this.status = Objects.requireNonNull(status, "status");
        this.affected = Objects.requireNonNull(affected, "affected");
    }

    public String getId() {
        return id;
    }

    public String getTheme() {
        return theme;
    }

    public String getProduct() {
        return product;
    }

    public LocalDate getStart() {
        return start;
    }

    public String getStatus() {
        return status;
    }

    public String getAffected() {
        return affected;
    }
}
