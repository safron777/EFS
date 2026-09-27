package ru.mtsbank.cardoffice.offers.domain;

/**
 * AD-10 stub-port pattern: the domain's boundary to the bank's external
 * offer/marketing engine (AD-1's own facade framing for {@code offers}).
 * The demo binds this to a stub adapter in {@code adapters/out/external/}
 * (see {@code offerengine.StubOfferEngineAdapter}); a later real integration
 * adds a new adapter implementation only -- this interface, and the domain
 * and application code depending on it, must not change.
 */
public interface OfferEnginePort {

    Offer getTopOffer(String clientId);
}
