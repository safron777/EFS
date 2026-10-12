package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.incidentcase.domain.Complaint;
import ru.mtsbank.cardoffice.incidentcase.domain.ComplaintRepository;

/**
 * Implements the domain {@link ComplaintRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link ComplaintEntity} (JPA) and {@link
 * Complaint} (domain).
 *
 * {@code term}/{@code status} defaults for a newly created row are fixed
 * here -- {@code "—"} is already a valid mock value and {@code "На 2
 * линии"} is the only non-terminal status the mock shows for this resource
 * (story Boundaries & Constraints).
 */
@Repository
public class JpaComplaintRepository implements ComplaintRepository {

    private static final String DEFAULT_TERM = "—";
    private static final String DEFAULT_STATUS = "На 2 линии";
    private static final String ID_PREFIX = "complaint-";

    private final ComplaintJpaRepository jpaRepository;

    public JpaComplaintRepository(ComplaintJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Complaint> findByClientId(String clientId) {
        return jpaRepository.findByClientIdOrderByCreatedDateDescIdAsc(clientId).stream()
                .map(JpaComplaintRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public Complaint create(String clientId, String theme) {
        String id = ID_PREFIX + jpaRepository.nextId();
        LocalDate created = LocalDate.now();
        ComplaintEntity entity = new ComplaintEntity(id, clientId, theme, created, DEFAULT_TERM, DEFAULT_STATUS);
        ComplaintEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private static Complaint toDomain(ComplaintEntity entity) {
        return new Complaint(
                entity.getId(), entity.getTheme(), entity.getCreatedDate(), entity.getTerm(), entity.getStatus());
    }
}
