package ru.mtsbank.cardoffice.offers.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.offers.domain.Offer;
import ru.mtsbank.cardoffice.offers.domain.OfferEnginePort;

/**
 * CAP-2: fetch a client's top offer. {@code clientId} is accepted at face
 * value and passed straight through to the port -- it is never verified
 * against {@code client-identity-service} (AD-9: no synchronous inter-service
 * calls) and there is no not-found case for this capability (story 2-offers
 * Execution notes), so this use case orchestrates the port with no branching
 * logic beyond the call.
 */
@Service
public class GetTopOfferUseCase {

    private final OfferEnginePort offerEnginePort;

    public GetTopOfferUseCase(OfferEnginePort offerEnginePort) {
        this.offerEnginePort = offerEnginePort;
    }

    public Offer execute(String clientId) {
        return offerEnginePort.getTopOffer(clientId);
    }
}
