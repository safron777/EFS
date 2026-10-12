package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncident;
import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncidentRepository;

/**
 * Implements the domain {@link SingleIncidentRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link SingleIncidentEntity} (JPA) and {@link
 * SingleIncident} (domain).
 *
 * {@code line}/{@code status} defaults for a newly created row are fixed
 * here -- the only values either field takes anywhere in the mock (story
 * Boundaries & Constraints).
 */
@Repository
public class JpaSingleIncidentRepository implements SingleIncidentRepository {

    private static final String DEFAULT_LINE = "2 линия";
    private static final String DEFAULT_STATUS = "В работе";
    private static final String ID_PREFIX = "ЕИ-";

    private final SingleIncidentJpaRepository jpaRepository;

    public JpaSingleIncidentRepository(SingleIncidentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SingleIncident> findByClientId(String clientId) {
        return jpaRepository.findByClientIdOrderByCreatedDateDescIdAsc(clientId).stream()
                .map(JpaSingleIncidentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public SingleIncident create(String clientId, String theme) {
        String id = ID_PREFIX + jpaRepository.nextId();
        LocalDate created = LocalDate.now();
        SingleIncidentEntity entity =
                new SingleIncidentEntity(id, clientId, theme, created, DEFAULT_LINE, DEFAULT_STATUS);
        SingleIncidentEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private static SingleIncident toDomain(SingleIncidentEntity entity) {
        return new SingleIncident(
                entity.getId(), entity.getTheme(), entity.getCreatedDate(), entity.getLine(), entity.getStatus());
    }
}
