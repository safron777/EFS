package ru.mtsbank.cardoffice.riskblocks.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Boots the real, DI-wired application (component scan finds the three
 * stub adapters / {@code GetRiskBlocksUseCase} / {@code
 * RiskBlocksController} exactly as production would) and exercises
 * {@code GET /cards/{cardId}/blocks} over the wire, covering all three
 * rows of story 7-risk-blocks' I/O & Edge-Case Matrix.
 *
 * {@code DataSourceAutoConfiguration}/{@code HibernateJpaAutoConfiguration}
 * are excluded: this capability persists nothing (story Boundaries &
 * Constraints -- no persistence, no Liquibase changeset, {@code
 * risk-blocks-service-postgres} stays provisioned but unused), so the test
 * must not require a real Postgres just to prove the business wiring
 * boots, mirroring {@code OfferControllerIntegrationTest}'s precedent.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        })
class RiskBlocksControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownCardReturns200MatchingMockDataFixtureWithArrestAmountAdded() {
        RiskBlocksResponse response = webTestClient()
                .get().uri("/cards/card-4019/blocks")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RiskBlocksResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        assertThat(response.sfm().title()).isEqualTo("Блокировка СФМ / Списки");
        assertThat(response.sfm().text())
                .isEqualTo("Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽");

        assertThat(response.arrest().title()).isEqualTo("Арест ФССП");
        assertThat(response.arrest().text())
                .isEqualTo("Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026");
        assertThat(response.arrest().amount()).isEqualTo(1_500_000L);

        assertThat(response.af().title()).isEqualTo("Блокировка АФ");
        assertThat(response.af().text()).isEqualTo("Карта •••• 7274 · неактивна");
    }

    @Test
    void differentCardReturnsIdenticalUnconditionalFixture() {
        RiskBlocksResponse response = webTestClient()
                .get().uri("/cards/card-7274/blocks")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RiskBlocksResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        assertThat(response.sfm().title()).isEqualTo("Блокировка СФМ / Списки");
        assertThat(response.sfm().text())
                .isEqualTo("Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽");

        assertThat(response.arrest().title()).isEqualTo("Арест ФССП");
        assertThat(response.arrest().text())
                .isEqualTo("Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026");
        assertThat(response.arrest().amount()).isEqualTo(1_500_000L);

        assertThat(response.af().title()).isEqualTo("Блокировка АФ");
        assertThat(response.af().text()).isEqualTo("Карта •••• 7274 · неактивна");
    }

    @Test
    void nonGetMethodReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .post().uri("/cards/card-4019/blocks")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
