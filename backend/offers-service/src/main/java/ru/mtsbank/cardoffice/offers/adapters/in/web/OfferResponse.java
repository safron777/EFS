package ru.mtsbank.cardoffice.offers.adapters.in.web;

import ru.mtsbank.cardoffice.offers.domain.Offer;

/**
 * {@code GET /clients/{clientId}/offers/top} response body. Mirrors
 * {@code mockData.offer}'s shape exactly (AD-8): {@code title}, {@code text},
 * {@code moreCount}.
 */
public record OfferResponse(String title, String text, int moreCount) {

    public static OfferResponse from(Offer offer) {
        return new OfferResponse(offer.getTitle(), offer.getText(), offer.getMoreCount());
    }
}
