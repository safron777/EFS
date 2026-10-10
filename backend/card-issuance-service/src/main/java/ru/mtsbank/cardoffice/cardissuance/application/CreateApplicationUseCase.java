package ru.mtsbank.cardoffice.cardissuance.application;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceApplication;
import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceRepository;

/**
 * CAP-5: the {@code POST .../issuance/applications} action -- a new endpoint
 * invented for this story (story Intent: "no client.js function exists for
 * this action at all"). {@code card}/{@code method} presence is validated
 * upstream by Jakarta Bean Validation on the request DTO (story Boundaries &
 * Constraints); this use case only assembles and persists the new row:
 * a generated {@link UUID} id (visibly different from the seeded
 * {@code appN} ids), today's server date, and the fixed status string
 * {@code "В обработке"} reused from {@code mockData.operations}' pending
 * state (story Boundaries & Constraints). Never touches
 * {@code currentApplication}/{@code newCard}.
 */
@Service
public class CreateApplicationUseCase {

    private static final String INITIAL_STATUS = "В обработке";

    private final IssuanceRepository issuanceRepository;

    public CreateApplicationUseCase(IssuanceRepository issuanceRepository) {
        this.issuanceRepository = issuanceRepository;
    }

    public IssuanceApplication execute(String card, String method) {
        IssuanceApplication application =
                new IssuanceApplication(UUID.randomUUID().toString(), LocalDate.now(), card, method, INITIAL_STATUS);
        return issuanceRepository.save(application);
    }
}
