package ru.mtsbank.cardoffice.incidentcase.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository port: the domain's view of mass-incident persistence,
 * implemented by an adapter in {@code adapters/out/persistence} (spine
 * Design Paradigm). The domain depends on this interface only, never on
 * JPA/Spring Data directly.
 *
 * This is the sole write surface for the read model {@code
 * MassIncidentEventListener} populates (AD-5) and the sole read surface for
 * CAP-10's {@code GET /incidents/mass} -- no other path reaches this
 * service's {@code mass_incidents}/{@code mass_incident_attachments}
 * tables.
 */
public interface MassIncidentRepository {

    List<MassIncident> findAll();

    /**
     * Applies an {@code opened} event. Idempotent by design (story
     * Boundaries & Constraints: Kafka's at-least-once delivery makes a
     * re-delivered {@code opened} for an {@code id} already on file a
     * normal, reachable case) -- upserts unconditionally, never throws.
     */
    void upsertOpened(String id, String theme, String product, LocalDate start, String status, String affected);

    /**
     * Applies an {@code updated} event, changing only {@code status}/{@code
     * affected}. An unknown {@code id} is logged and skipped rather than
     * thrown (story Boundaries & Constraints: topic ordering makes an
     * {@code updated} for an unknown incident a reachable case in normal
     * operation, not just a theoretical edge).
     */
    void applyUpdate(String id, String status, String affected);

    /**
     * Applies a {@code closed} event, changing only {@code status}. Same
     * unknown-{@code id} handling as {@link #applyUpdate}.
     */
    void applyClose(String id, String status);

    /**
     * Records {@code clientId} against {@code incidentId}. Idempotent on a
     * repeat attach of the same pair (story Boundaries & Constraints: no
     * duplicate row, no error) -- the UI lets an employee click the button
     * more than once.
     *
     * @throws MassIncidentNotFoundException if no incident with that id
     *     exists
     */
    void attachClient(String incidentId, String clientId);
}
