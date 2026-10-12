package ru.mtsbank.cardoffice.incidentcase.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.incidentcase.domain.Complaint;
import ru.mtsbank.cardoffice.incidentcase.domain.ComplaintRepository;

/**
 * CAP-11: the {@code POST /clients/{clientId}/complaints} action, mirroring
 * {@code client.js}'s {@code createComplaint(clientId, payload)} contract
 * (story Intent). {@code theme} presence/length is validated upstream by
 * Jakarta Bean Validation on the request DTO (story Boundaries &
 * Constraints); this use case only assembles and persists the new row via
 * the repository port, which assigns the {@code complaint-<n>} id, {@code
 * term}, and {@code status} defaults.
 */
@Service
public class CreateComplaintUseCase {

    private final ComplaintRepository complaintRepository;

    public CreateComplaintUseCase(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public Complaint execute(String clientId, String theme) {
        return complaintRepository.create(clientId, theme);
    }
}
