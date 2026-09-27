package ru.mtsbank.cardoffice.offers.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.offers.application.GetTopOfferUseCase;
import ru.mtsbank.cardoffice.offers.domain.Offer;

/**
 * CAP-2 REST resource: {@code GET /clients/{clientId}/offers/top} (AD-8).
 * Read-only -- no mutation endpoints belong here (story Boundaries &
 * Constraints). Translates HTTP to the application layer only; no business
 * logic lives here (spine Design Paradigm).
 */
@RestController
@RequestMapping("/clients")
public class OfferController {

    private final GetTopOfferUseCase getTopOfferUseCase;

    public OfferController(GetTopOfferUseCase getTopOfferUseCase) {
        this.getTopOfferUseCase = getTopOfferUseCase;
    }

    @GetMapping("/{clientId}/offers/top")
    public OfferResponse getTopOffer(@PathVariable("clientId") String clientId) {
        Offer offer = getTopOfferUseCase.execute(clientId);
        return OfferResponse.from(offer);
    }
}
