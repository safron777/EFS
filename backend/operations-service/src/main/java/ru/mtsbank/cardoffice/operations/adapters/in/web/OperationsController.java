package ru.mtsbank.cardoffice.operations.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.operations.application.GetOperationsUseCase;

/**
 * CAP-6 REST resource: {@code GET /cards/{cardId}/operations} (AD-8),
 * mirroring {@code client.js}'s {@code getOperations} exactly. Read-only --
 * no mutation endpoints belong here (story Boundaries & Constraints).
 * Translates HTTP to the application layer only; no business logic lives
 * here (spine Design Paradigm).
 *
 * An unknown {@code cardId} is not a {@code 404}: this is a
 * collection-shaped endpoint, so it returns {@code 200} with {@code
 * {failedAlert: null, rows: []}} (story Boundaries & Constraints), the same
 * 200-empty precedent {@code CardController} set for an unmatched {@code
 * clientId}/{@code productId}.
 */
@RestController
public class OperationsController {

    private final GetOperationsUseCase getOperationsUseCase;

    public OperationsController(GetOperationsUseCase getOperationsUseCase) {
        this.getOperationsUseCase = getOperationsUseCase;
    }

    @GetMapping("/cards/{cardId}/operations")
    public OperationsResponse getOperations(@PathVariable("cardId") String cardId) {
        return OperationsResponse.from(getOperationsUseCase.execute(cardId));
    }
}
