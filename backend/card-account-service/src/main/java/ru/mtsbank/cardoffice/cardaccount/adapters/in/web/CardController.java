package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.cardaccount.application.GetCardsUseCase;

/**
 * CAP-3 REST resource: {@code GET
 * /clients/{clientId}/products/{productId}/cards} (AD-8). Read-only -- no
 * mutation endpoints belong here (story Boundaries & Constraints: "switching"
 * the active card is a frontend-only concern). Translates HTTP to the
 * application layer only; no business logic lives here (spine Design
 * Paradigm).
 *
 * An unknown {@code clientId}/{@code productId} is not a 404: this is a
 * collection endpoint, so it returns {@code 200} with an empty array,
 * standard REST collection semantics (story Boundaries & Constraints).
 */
@RestController
@RequestMapping("/clients/{clientId}/products/{productId}/cards")
public class CardController {

    private final GetCardsUseCase getCardsUseCase;

    public CardController(GetCardsUseCase getCardsUseCase) {
        this.getCardsUseCase = getCardsUseCase;
    }

    @GetMapping
    public List<CardResponse> getCards(
            @PathVariable("clientId") String clientId, @PathVariable("productId") String productId) {
        return getCardsUseCase.execute(clientId, productId).stream()
                .map(CardResponse::from)
                .toList();
    }
}
