package ru.mtsbank.cardoffice.cardissuance.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardissuance.domain.CurrentApplication;
import ru.mtsbank.cardoffice.cardissuance.domain.Issuance;
import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceRepository;
import ru.mtsbank.cardoffice.cardissuance.domain.NewCardInfo;

/**
 * CAP-5: fetch the issuance state for a client/product -- the static
 * {@code currentApplication}/{@code newCard} fixtures plus the real,
 * persisted {@code history}. {@code clientId}/{@code productId} are accepted
 * at face value and never reach the repository query: history is global in
 * this demo (story Boundaries & Constraints), same simplification
 * {@code GetDocumentsUseCase} made in card-account-service.
 */
@Service
public class GetIssuanceUseCase {

    private final IssuanceRepository issuanceRepository;

    public GetIssuanceUseCase(IssuanceRepository issuanceRepository) {
        this.issuanceRepository = issuanceRepository;
    }

    public Issuance execute(String clientId, String productId) {
        return new Issuance(CurrentApplication.fixture(), NewCardInfo.fixture(), issuanceRepository.findAllOrdered());
    }
}
