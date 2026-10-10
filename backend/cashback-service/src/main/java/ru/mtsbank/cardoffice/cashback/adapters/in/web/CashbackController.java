package ru.mtsbank.cardoffice.cashback.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.cashback.application.GetCashbackUseCase;

/**
 * CAP-8 REST resource: {@code GET /cards/{cardId}/cashback} (AD-8),
 * mirroring {@code client.js}'s {@code getCashback} exactly. Read-only --
 * no mutation endpoints belong here (story Boundaries & Constraints).
 * Translates HTTP to the application layer only; no business logic lives
 * here (spine Design Paradigm).
 *
 * {@code cardId} is never validated against {@code card-account-service}
 * (AD-9) -- it is accepted as-is and passed straight through, and never
 * used to filter the one demo-wide fixture (story Boundaries &
 * Constraints).
 */
@RestController
public class CashbackController {

    private final GetCashbackUseCase getCashbackUseCase;

    public CashbackController(GetCashbackUseCase getCashbackUseCase) {
        this.getCashbackUseCase = getCashbackUseCase;
    }

    @GetMapping("/cards/{cardId}/cashback")
    public CashbackResponse getCashback(@PathVariable("cardId") String cardId) {
        return CashbackResponse.from(getCashbackUseCase.execute(cardId));
    }
}
