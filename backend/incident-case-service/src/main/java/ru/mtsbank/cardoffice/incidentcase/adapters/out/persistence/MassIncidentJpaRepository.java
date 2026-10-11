package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link MassIncidentEntity}, used only by
 * {@link JpaMassIncidentRepository}. Ordered by {@code start_date} descending
 * (most recently opened first), with {@code id} as a stable tie-breaker --
 * the mock's own {@code massIncidents} array order (same precedent as
 * {@code NotificationChannelJpaRepository}'s {@code sort_order} ordering).
 */
public interface MassIncidentJpaRepository extends JpaRepository<MassIncidentEntity, String> {

    List<MassIncidentEntity> findAllByOrderByStartDateDescIdAsc();
}
