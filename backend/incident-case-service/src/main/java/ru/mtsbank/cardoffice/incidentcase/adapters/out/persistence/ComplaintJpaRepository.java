package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository over {@link ComplaintEntity}, used only by {@link
 * JpaComplaintRepository}. Ordered by {@code created_date} descending (most
 * recent first), with {@code id} as a stable tie-breaker -- same precedent
 * as {@code MassIncidentJpaRepository}.
 */
public interface ComplaintJpaRepository extends JpaRepository<ComplaintEntity, String> {

    List<ComplaintEntity> findByClientIdOrderByCreatedDateDescIdAsc(String clientId);

    /**
     * Draws the next value from this table's own {@code complaint_id_seq}
     * (db/changelog 002), started comfortably above the seeded rows' own
     * numbers purely so a freshly created id is visually distinct from them
     * (story Boundaries & Constraints) -- the generation scheme behind the
     * {@code complaint-<n>} id {@link JpaComplaintRepository#create}
     * assigns.
     */
    @Query(value = "select nextval('complaint_id_seq')", nativeQuery = true)
    long nextId();
}
