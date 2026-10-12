package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.incidentcase.application.CreateSingleIncidentUseCase;
import ru.mtsbank.cardoffice.incidentcase.application.GetSingleIncidentsUseCase;
import ru.mtsbank.cardoffice.incidentcase.domain.SingleIncident;

/**
 * CAP-11 REST resource: {@code GET}/{@code POST
 * /clients/{clientId}/incidents/single} (AD-8), mirroring {@code
 * client.js}'s {@code getSingleIncidents}/{@code createSingleIncident}
 * exactly. Translates HTTP to the application layer only; no business logic
 * lives here (spine Design Paradigm).
 *
 * {@code clientId} is accepted at face value and never cross-checked
 * against {@code client-identity-service} (AD-9); an unseeded/unknown
 * {@code clientId} on {@code GET} returns {@code 200 []}, never {@code 404}
 * (story Boundaries & Constraints).
 */
@RestController
@RequestMapping("/clients/{clientId}/incidents/single")
public class SingleIncidentController {

    private final GetSingleIncidentsUseCase getSingleIncidentsUseCase;
    private final CreateSingleIncidentUseCase createSingleIncidentUseCase;

    public SingleIncidentController(
            GetSingleIncidentsUseCase getSingleIncidentsUseCase,
            CreateSingleIncidentUseCase createSingleIncidentUseCase) {
        this.getSingleIncidentsUseCase = getSingleIncidentsUseCase;
        this.createSingleIncidentUseCase = createSingleIncidentUseCase;
    }

    @GetMapping
    public List<SingleIncidentResponse> getSingleIncidents(@PathVariable("clientId") String clientId) {
        return getSingleIncidentsUseCase.execute(clientId).stream().map(SingleIncidentResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SingleIncidentResponse> createSingleIncident(
            @PathVariable("clientId") String clientId, @Valid @RequestBody CreateSingleIncidentRequest request) {
        SingleIncident created = createSingleIncidentUseCase.execute(clientId, request.theme());
        return ResponseEntity.status(HttpStatus.CREATED).body(SingleIncidentResponse.from(created));
    }
}
