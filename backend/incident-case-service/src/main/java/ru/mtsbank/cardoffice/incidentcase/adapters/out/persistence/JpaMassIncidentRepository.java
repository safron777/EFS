package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncident;
import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentNotFoundException;
import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentRepository;

/**
 * Implements the domain {@link MassIncidentRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link MassIncidentEntity}/{@link
 * MassIncidentAttachmentEntity} (JPA) and {@link MassIncident} (domain).
 *
 * This is the one place the idempotency/unknown-id rules from the story's
 * Boundaries & Constraints are actually enforced: {@link #upsertOpened}
 * always succeeds (a re-delivered {@code opened} just overwrites the same
 * row); {@link #applyUpdate}/{@link #applyClose} log and skip an unknown
 * {@code id} rather than throwing (Kafka's at-least-once delivery and topic
 * ordering make both reachable in normal operation); {@link #attachClient}
 * is idempotent on a repeat attach because {@link MassIncidentAttachmentEntity}'s
 * composite primary key makes a repeat {@code save} a plain update of the
 * same row, never a duplicate insert.
 */
@Repository
public class JpaMassIncidentRepository implements MassIncidentRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaMassIncidentRepository.class);

    private final MassIncidentJpaRepository massIncidentJpaRepository;
    private final MassIncidentAttachmentJpaRepository attachmentJpaRepository;

    public JpaMassIncidentRepository(
            MassIncidentJpaRepository massIncidentJpaRepository,
            MassIncidentAttachmentJpaRepository attachmentJpaRepository) {
        this.massIncidentJpaRepository = massIncidentJpaRepository;
        this.attachmentJpaRepository = attachmentJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MassIncident> findAll() {
        return massIncidentJpaRepository.findAllByOrderByStartDateDescIdAsc().stream()
                .map(JpaMassIncidentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void upsertOpened(
            String id, String theme, String product, LocalDate start, String status, String affected) {
        Optional<MassIncidentEntity> existing = massIncidentJpaRepository.findById(id);
        if (existing.isPresent()) {
            // Idempotent re-delivery (story Boundaries & Constraints) -- overwrite with
            // the freshly delivered fields rather than rejecting or ignoring.
            log.info("Re-applying opened event for already-known mass incident {}", id);
            MassIncidentEntity entity = existing.get();
            entity.setTheme(theme);
            entity.setProduct(product);
            entity.setStartDate(start);
            entity.setStatus(status);
            entity.setAffected(affected);
            massIncidentJpaRepository.save(entity);
        } else {
            massIncidentJpaRepository.save(new MassIncidentEntity(id, theme, product, start, status, affected));
        }
    }

    @Override
    @Transactional
    public void applyUpdate(String id, String status, String affected) {
        Optional<MassIncidentEntity> existing = massIncidentJpaRepository.findById(id);
        if (existing.isEmpty()) {
            // Unknown id -- logged and skipped, never thrown (story Boundaries &
            // Constraints: topic ordering makes this reachable in normal operation).
            log.warn("Ignoring updated event for unknown mass incident {}", id);
            return;
        }
        MassIncidentEntity entity = existing.get();
        entity.setStatus(status);
        entity.setAffected(affected);
        massIncidentJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void applyClose(String id, String status) {
        Optional<MassIncidentEntity> existing = massIncidentJpaRepository.findById(id);
        if (existing.isEmpty()) {
            log.warn("Ignoring closed event for unknown mass incident {}", id);
            return;
        }
        MassIncidentEntity entity = existing.get();
        entity.setStatus(status);
        massIncidentJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void attachClient(String incidentId, String clientId) {
        if (!massIncidentJpaRepository.existsById(incidentId)) {
            throw new MassIncidentNotFoundException(incidentId);
        }
        // A repeat attach of the same (incidentId, clientId) pair re-saves the same
        // composite-key row (merge), not a duplicate insert -- see class Javadoc.
        attachmentJpaRepository.save(new MassIncidentAttachmentEntity(incidentId, clientId, Instant.now()));
    }

    private static MassIncident toDomain(MassIncidentEntity entity) {
        return new MassIncident(
                entity.getId(),
                entity.getTheme(),
                entity.getProduct(),
                entity.getStartDate(),
                entity.getStatus(),
                entity.getAffected());
    }
}
