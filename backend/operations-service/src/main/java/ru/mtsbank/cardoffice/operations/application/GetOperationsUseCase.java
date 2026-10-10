package ru.mtsbank.cardoffice.operations.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.operations.domain.OperationsHistory;
import ru.mtsbank.cardoffice.operations.domain.OperationsRepository;

/**
 * CAP-6: fetch a card's operation history (alert + ordered rows). Pure
 * pass-through over the {@link OperationsRepository} port -- no not-found
 * branching, since an unmatched {@code cardId} is an ordinary empty result
 * for this collection-shaped endpoint, not an error (story Boundaries &
 * Constraints), the same simplification {@code GetCardsUseCase} made in
 * card-account-service.
 */
@Service
public class GetOperationsUseCase {

    private final OperationsRepository operationsRepository;

    public GetOperationsUseCase(OperationsRepository operationsRepository) {
        this.operationsRepository = operationsRepository;
    }

    public OperationsHistory execute(String cardId) {
        return operationsRepository.getHistory(cardId);
    }
}
