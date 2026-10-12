package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository over {@link SingleIncidentEntity}, used only by
 * {@link JpaSingleIncidentRepository}. Ordered by {@code created_date}
 * descending (most recent first), with {@code id} as a stable tie-breaker
 * -- same precedent as {@code MassIncidentJpaRepository}.
 */
public interface SingleIncidentJpaRepository extends JpaRepository<SingleIncidentEntity, String> {

    List<SingleIncidentEntity> findByClientIdOrderByCreatedDateDescIdAsc(String clientId);

    /**
     * Draws the next value from this table's own {@code
     * single_incident_id_seq} (db/changelog 002), started comfortably above
     * the seeded rows' own numbers purely so a freshly created id is
     * visually distinct from them (story Boundaries & Constraints) -- the
     * generation scheme behind the {@code ЕИ-<n>} id {@link
     * JpaSingleIncidentRepository#create} assigns.
     */
    @Query(value = "select nextval('single_incident_id_seq')", nativeQuery = true)
    long nextId();
}
