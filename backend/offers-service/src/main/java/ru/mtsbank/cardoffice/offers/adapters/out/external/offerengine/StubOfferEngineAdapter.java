package ru.mtsbank.cardoffice.offers.adapters.out.external.offerengine;

import org.springframework.stereotype.Component;

import ru.mtsbank.cardoffice.offers.domain.Offer;
import ru.mtsbank.cardoffice.offers.domain.OfferEnginePort;

/**
 * AD-10 stub adapter: stands in for the bank's real external offer/marketing
 * engine, which is Deferred in the spine. Returns the fixed fixture from
 * {@code src/api/mockData.js}'s {@code offer} export regardless of
 * {@code clientId} -- unconditional, matching today's mock (story 2-offers
 * I/O & Edge-Case Matrix: "not yet personalized").
 */
@Component
public class StubOfferEngineAdapter implements OfferEnginePort {

    private static final String FIXTURE_TITLE = "MTS CASHBACK 111 дней";
    private static final String FIXTURE_TEXT = "Повышенный кэшбэк на 111 дней при активации подписки";
    private static final int FIXTURE_MORE_COUNT = 1;

    @Override
    public Offer getTopOffer(String clientId) {
        return new Offer(FIXTURE_TITLE, FIXTURE_TEXT, FIXTURE_MORE_COUNT);
    }
}
