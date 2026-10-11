package ru.mtsbank.cardoffice.incidentcase.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentNotFoundException;
import ru.mtsbank.cardoffice.incidentcase.domain.MassIncidentRepository;

/**
 * CAP-10: {@code attachClientToIncident(clientId, incidentId)} wired to a
 * real mutation (story Intent). {@code clientId} is never validated against
 * {@code client-identity-service} (AD-9) -- it is recorded as-is.
 */
@Service
public class AttachClientUseCase {

    private final MassIncidentRepository massIncidentRepository;

    public AttachClientUseCase(MassIncidentRepository massIncidentRepository) {
        this.massIncidentRepository = massIncidentRepository;
    }

    /**
     * @throws MassIncidentNotFoundException if no incident with that id
     *     exists
     */
    public void execute(String incidentId, String clientId) {
        massIncidentRepository.attachClient(incidentId, clientId);
    }
}
