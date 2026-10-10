package ru.mtsbank.cardoffice.riskblocks.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.riskblocks.application.GetRiskBlocksUseCase;

/**
 * CAP-7 REST resource: {@code GET /cards/{cardId}/blocks} (AD-8),
 * mirroring {@code client.js}'s {@code getBlocks} exactly. Read-only -- no
 * mutation endpoints belong here (story Boundaries & Constraints).
 * Translates HTTP to the application layer only; no business logic lives
 * here (spine Design Paradigm).
 *
 * {@code cardId} is never validated against {@code card-account-service}
 * (AD-9) -- it is accepted as-is and passed straight through.
 */
@RestController
public class RiskBlocksController {

    private final GetRiskBlocksUseCase getRiskBlocksUseCase;

    public RiskBlocksController(GetRiskBlocksUseCase getRiskBlocksUseCase) {
        this.getRiskBlocksUseCase = getRiskBlocksUseCase;
    }

    @GetMapping("/cards/{cardId}/blocks")
    public RiskBlocksResponse getBlocks(@PathVariable("cardId") String cardId) {
        return RiskBlocksResponse.from(getRiskBlocksUseCase.execute(cardId));
    }
}
