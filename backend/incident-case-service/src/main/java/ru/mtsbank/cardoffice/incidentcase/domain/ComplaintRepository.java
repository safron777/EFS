package ru.mtsbank.cardoffice.incidentcase.domain;

import java.util.List;

/**
 * Repository port: the domain's view of complaint persistence, implemented
 * by an adapter in {@code adapters/out/persistence} (spine Design
 * Paradigm). The domain depends on this interface only, never on JPA/Spring
 * Data directly.
 *
 * CAP-11: client-scoped CRUD-lite, no Kafka, no stub ports (story
 * Approach). {@code clientId} is accepted at face value and never
 * cross-checked against {@code client-identity-service} (AD-9).
 */
public interface ComplaintRepository {

    /** Rows for {@code clientId}, empty (never an error) if none exist (story Boundaries & Constraints). */
    List<Complaint> findByClientId(String clientId);

    /**
     * Creates and persists a new complaint for {@code clientId} with the
     * given {@code theme}, assigning a fresh {@code complaint-<n>} id from
     * this table's own sequence, {@code term: "—"}, {@code status: "На 2
     * линии"} (story Boundaries & Constraints).
     */
    Complaint create(String clientId, String theme);
}
