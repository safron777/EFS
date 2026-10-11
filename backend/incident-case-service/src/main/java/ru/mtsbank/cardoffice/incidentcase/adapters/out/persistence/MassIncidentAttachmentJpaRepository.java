package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link MassIncidentAttachmentEntity}, used
 * only by {@link JpaMassIncidentRepository}.
 */
public interface MassIncidentAttachmentJpaRepository
        extends JpaRepository<MassIncidentAttachmentEntity, MassIncidentAttachmentEntity.Key> {
}
