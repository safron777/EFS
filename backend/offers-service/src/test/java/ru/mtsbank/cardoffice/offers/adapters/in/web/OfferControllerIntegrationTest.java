package ru.mtsbank.cardoffice.offers.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Boots the real, DI-wired application (component scan finds
 * {@code StubOfferEngineAdapter}/{@code GetTopOfferUseCase}/{@code OfferController}
 * exactly as production would) and exercises {@code GET
 * /clients/{clientId}/offers/top} over the wire -- unlike
 * {@code OfferControllerTest} and {@code StubOfferEngineAdapterTest}, which
 * each construct their subject directly and would stay green even if a
 * {@code @Component}/{@code @Service} annotation were dropped and wiring
 * broke at real boot.
 *
 * {@code DataSourceAutoConfiguration}/{@code HibernateJpaAutoConfiguration}
 * are excluded: this capability persists nothing (story 2-offers Boundaries
 * & Constraints -- "no JPA, no Liquibase, no persistence"), so the test must
 * not require a real Postgres just to prove the business wiring boots.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        })
class OfferControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownClientReturnsFixtureThroughRealDiWiredApp() {
        OfferResponse response = webTestClient()
                .get().uri("/clients/client-1/offers/top")
                .exchange()
                .expectStatus().isOk()
                .expectBody(OfferResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("MTS CASHBACK 111 дней");
        assertThat(response.text()).isEqualTo("Повышенный кэшбэк на 111 дней при активации подписки");
        assertThat(response.moreCount()).isEqualTo(1);
    }

    @Test
    void differentClientReturnsSameFixtureUnchanged() {
        OfferResponse response = webTestClient()
                .get().uri("/clients/client-2/offers/top")
                .exchange()
                .expectStatus().isOk()
                .expectBody(OfferResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("MTS CASHBACK 111 дней");
        assertThat(response.text()).isEqualTo("Повышенный кэшбэк на 111 дней при активации подписки");
        assertThat(response.moreCount()).isEqualTo(1);
    }
}
