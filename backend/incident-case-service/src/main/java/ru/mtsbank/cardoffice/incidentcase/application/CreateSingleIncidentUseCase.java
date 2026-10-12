package ru.mtsbank.cardoffice.incidentcase.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncident;
import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncidentRepository;

/**
 * CAP-11: the {@code POST /clients/{clientId}/incidents/single} action,
 * mirroring {@code client.js}'s {@code createSingleIncident(clientId,
 * payload)} contract (story Intent). {@code theme} presence/length is
 * validated upstream by Jakarta Bean Validation on the request DTO (story
 * Boundaries & Constraints); this use case only assembles and persists the
 * new row via the repository port, which assigns the {@code ЕИ-<n>} id,
 * {@code line}, and {@code status} defaults.
 */
@Service
public class CreateSingleIncidentUseCase {

    private final SingleIncidentRepository singleIncidentRepository;

    public CreateSingleIncidentUseCase(SingleIncidentRepository singleIncidentRepository) {
        this.singleIncidentRepository = singleIncidentRepository;
    }

    public SingleIncident execute(String clientId, String theme) {
        return singleIncidentRepository.create(clientId, theme);
    }
}
