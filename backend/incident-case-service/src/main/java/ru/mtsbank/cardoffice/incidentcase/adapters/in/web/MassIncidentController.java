package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.incidentcase.application.AttachClientUseCase;
import ru.mtsbank.cardoffice.incidentcase.application.GetMassIncidentsUseCase;

/**
 * CAP-10 REST resource: {@code GET /incidents/mass} and {@code POST
 * /incidents/mass/{incidentId}/attach} (AD-8), mirroring {@code client.js}'s
 * {@code getMassIncidents}/{@code attachClientToIncident} exactly.
 * Translates HTTP to the application layer only; no business logic lives
 * here (spine Design Paradigm).
 *
 * Both the read model and the attach mutation are populated/read
 * exclusively through {@code MassIncidentRepository} -- no synchronous call
 * to any other service, ever (AD-9), and no REST call to the monitoring
 * system (AD-5): the read model exists only because {@code
 * MassIncidentEventListener} already wrote it from Kafka.
 */
@RestController
public class MassIncidentController {

    private final GetMassIncidentsUseCase getMassIncidentsUseCase;
    private final AttachClientUseCase attachClientUseCase;

    public MassIncidentController(
            GetMassIncidentsUseCase getMassIncidentsUseCase, AttachClientUseCase attachClientUseCase) {
        this.getMassIncidentsUseCase = getMassIncidentsUseCase;
        this.attachClientUseCase = attachClientUseCase;
    }

    @GetMapping("/incidents/mass")
    public List<MassIncidentResponse> getMassIncidents() {
        return getMassIncidentsUseCase.execute().stream().map(MassIncidentResponse::from).toList();
    }

    @PostMapping("/incidents/mass/{incidentId}/attach")
    public OkResponse attach(
            @PathVariable("incidentId") String incidentId, @Valid @RequestBody AttachClientRequest request) {
        attachClientUseCase.execute(incidentId, request.clientId());
        return OkResponse.OK;
    }
}
