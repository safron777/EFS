package ru.mtsbank.cardoffice.incidentcase.domain;

import java.util.List;

/**
 * Repository port: the domain's view of single-incident persistence,
 * implemented by an adapter in {@code adapters/out/persistence} (spine
 * Design Paradigm). The domain depends on this interface only, never on
 * JPA/Spring Data directly.
 *
 * CAP-11: client-scoped CRUD-lite, no Kafka, no stub ports (story
 * Approach). {@code clientId} is accepted at face value and never
 * cross-checked against {@code client-identity-service} (AD-9).
 */
public interface SingleIncidentRepository {

    /** Rows for {@code clientId}, empty (never an error) if none exist (story Boundaries & Constraints). */
    List<SingleIncident> findByClientId(String clientId);

    /**
     * Creates and persists a new single incident for {@code clientId} with
     * the given {@code theme}, assigning a fresh {@code ЕИ-<n>} id from this
     * table's own sequence, {@code line: "2 линия"}, {@code status: "В
     * работе"} (story Boundaries & Constraints).
     */
    SingleIncident create(String clientId, String theme);
}
