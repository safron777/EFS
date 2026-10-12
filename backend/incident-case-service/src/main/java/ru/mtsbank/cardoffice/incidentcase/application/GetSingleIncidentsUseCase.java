package ru.mtsbank.cardoffice.incidentcase.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncident;
import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncidentRepository;

/**
 * CAP-11: fetch a client's single incidents for {@code GET
 * /clients/{clientId}/incidents/single}. Pure pass-through over the {@link
 * SingleIncidentRepository} port -- an unseeded/unknown {@code clientId}
 * yields an empty list, never an error (story Boundaries & Constraints).
 */
@Service
public class GetSingleIncidentsUseCase {

    private final SingleIncidentRepository singleIncidentRepository;

    public GetSingleIncidentsUseCase(SingleIncidentRepository singleIncidentRepository) {
        this.singleIncidentRepository = singleIncidentRepository;
    }

    public List<SingleIncident> execute(String clientId) {
        return singleIncidentRepository.findByClientId(clientId);
    }
}
