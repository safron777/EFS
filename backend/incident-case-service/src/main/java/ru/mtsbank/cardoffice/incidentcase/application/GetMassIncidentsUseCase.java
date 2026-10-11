package ru.mtsbank.cardoffice.incidentcase.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncident;
import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentRepository;

/**
 * CAP-10: fetch the mass-incidents read model for {@code GET
 * /incidents/mass}. Pure pass-through over the {@link MassIncidentRepository}
 * port -- a global, unscoped list, no {@code clientId} parameter (story
 * Intent).
 */
@Service
public class GetMassIncidentsUseCase {

    private final MassIncidentRepository massIncidentRepository;

    public GetMassIncidentsUseCase(MassIncidentRepository massIncidentRepository) {
        this.massIncidentRepository = massIncidentRepository;
    }

    public List<MassIncident> execute() {
        return massIncidentRepository.findAll();
    }
}
