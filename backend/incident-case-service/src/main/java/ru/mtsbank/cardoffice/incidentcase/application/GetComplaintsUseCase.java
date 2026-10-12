package ru.mtsbank.cardoffice.incidentcase.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.Complaint;
import ru.mtsbank.cardoffice.incidentcase.domain.ComplaintRepository;

/**
 * CAP-11: fetch a client's complaints for {@code GET
 * /clients/{clientId}/complaints}. Pure pass-through over the {@link
 * ComplaintRepository} port -- an unseeded/unknown {@code clientId} yields
 * an empty list, never an error (story Boundaries & Constraints).
 */
@Service
public class GetComplaintsUseCase {

    private final ComplaintRepository complaintRepository;

    public GetComplaintsUseCase(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public List<Complaint> execute(String clientId) {
        return complaintRepository.findByClientId(clientId);
    }
}
