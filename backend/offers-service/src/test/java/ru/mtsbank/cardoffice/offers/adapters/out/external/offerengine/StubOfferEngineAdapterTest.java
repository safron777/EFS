package ru.mtsbank.cardoffice.offers.adapters.out.external.offerengine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ru.mtsbank.cardoffice.offers.domain.Offer;

/**
 * Plain-Java unit test -- no Spring context, no database needed (story
 * 2-offers deliberately keeps this capability schema-free). Covers both rows
 * of the story's I/O & Edge-Case Matrix directly against the adapter: the
 * fixture is returned unconditionally, unchanged across different client ids.
 */
class StubOfferEngineAdapterTest {

    private final StubOfferEngineAdapter adapter = new StubOfferEngineAdapter();

    @Test
    void returnsMockDataOfferFixtureForAnyClient() {
        Offer offer = adapter.getTopOffer("client-1");

        assertThat(offer.getTitle()).isEqualTo("MTS CASHBACK 111 дней");
        assertThat(offer.getText()).isEqualTo("Повышенный кэшбэк на 111 дней при активации подписки");
        assertThat(offer.getMoreCount()).isEqualTo(1);
    }

    @Test
    void returnsSameFixtureUnchangedForADifferentClient() {
        Offer first = adapter.getTopOffer("client-1");
        Offer second = adapter.getTopOffer("client-2");

        assertThat(second.getTitle()).isEqualTo(first.getTitle());
        assertThat(second.getText()).isEqualTo(first.getText());
        assertThat(second.getMoreCount()).isEqualTo(first.getMoreCount());
    }
}
