package ru.mtsbank.cardoffice.incidentcase.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentRepository;

/**
 * CAP-10: the one use case all three {@code MassIncidentEventListener}
 * methods call (story Code Map/Tasks), dispatching to the right {@link
 * MassIncidentRepository} method per event type. Kept as typed per-verb
 * methods rather than a single method branching on {@code eventType} --
 * each event's {@code data} shape is already distinct by the time it
 * reaches here (the listener has already validated the envelope's {@code
 * version} and converted {@code data} to the matching typed record), so
 * there is nothing left to branch on.
 */
@Service
public class ApplyMassIncidentEventUseCase {

    private final MassIncidentRepository massIncidentRepository;

    public ApplyMassIncidentEventUseCase(MassIncidentRepository massIncidentRepository) {
        this.massIncidentRepository = massIncidentRepository;
    }

    /** Applies a validated {@code opened} event's {@code data} fields. */
    public void applyOpened(String id, String theme, String product, LocalDate start, String status, String affected) {
        massIncidentRepository.upsertOpened(id, theme, product, start, status, affected);
    }

    /** Applies a validated {@code updated} event's {@code data} fields. */
    public void applyUpdated(String id, String status, String affected) {
        massIncidentRepository.applyUpdate(id, status, affected);
    }

    /** Applies a validated {@code closed} event's {@code data} fields. */
    public void applyClosed(String id, String status) {
        massIncidentRepository.applyClose(id, status);
    }
}
