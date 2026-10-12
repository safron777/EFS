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

import ru.mtsbank.cardoffice.incidentcase.application.CreateComplaintUseCase;
import ru.mtsbank.cardoffice.incidentcase.application.GetComplaintsUseCase;
import ru.mtsbank.cardoffice.incidentcase.domain.Complaint;

/**
 * CAP-11 REST resource: {@code GET}/{@code POST
 * /clients/{clientId}/complaints} (AD-8), mirroring {@code client.js}'s
 * {@code getComplaints}/{@code createComplaint} exactly. Translates HTTP to
 * the application layer only; no business logic lives here (spine Design
 * Paradigm).
 *
 * {@code clientId} is accepted at face value and never cross-checked
 * against {@code client-identity-service} (AD-9); an unseeded/unknown
 * {@code clientId} on {@code GET} returns {@code 200 []}, never {@code 404}
 * (story Boundaries & Constraints).
 */
@RestController
@RequestMapping("/clients/{clientId}/complaints")
public class ComplaintController {

    private final GetComplaintsUseCase getComplaintsUseCase;
    private final CreateComplaintUseCase createComplaintUseCase;

    public ComplaintController(
            GetComplaintsUseCase getComplaintsUseCase, CreateComplaintUseCase createComplaintUseCase) {
        this.getComplaintsUseCase = getComplaintsUseCase;
        this.createComplaintUseCase = createComplaintUseCase;
    }

    @GetMapping
    public List<ComplaintResponse> getComplaints(@PathVariable("clientId") String clientId) {
        return getComplaintsUseCase.execute(clientId).stream().map(ComplaintResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @PathVariable("clientId") String clientId, @Valid @RequestBody CreateComplaintRequest request) {
        Complaint created = createComplaintUseCase.execute(clientId, request.theme());
        return ResponseEntity.status(HttpStatus.CREATED).body(ComplaintResponse.from(created));
    }
}
