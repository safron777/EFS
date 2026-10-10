package ru.mtsbank.cardoffice.cashback.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers all three rows of story 8-cashback's I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions), mirroring {@code DocumentControllerIntegrationTest}'s
 * shape. {@code @ServiceConnection} points Spring Boot's datasource at the
 * container, so Liquibase runs the actual changelog (001/002) on every
 * test run.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashbackControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownCardReturns200MatchingMockDataFixtureWithAccumulatedInKopecks() {
        CashbackResponse response = webTestClient()
                .get().uri("/cards/card-4019/cashback")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CashbackResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertFixtureMatches(response);
    }

    @Test
    void differentCardReturnsIdenticalUnconditionalFixture() {
        CashbackResponse response = webTestClient()
                .get().uri("/cards/card-7274/cashback")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CashbackResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertFixtureMatches(response);
    }

    @Test
    void nonGetMethodReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .post().uri("/cards/card-4019/cashback")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    private static void assertFixtureMatches(CashbackResponse response) {
        // AD-3: the mock's raw-ruble 1240 becomes the signed kopecks integer 124000.
        assertThat(response.accumulated()).isEqualTo(124_000L);

        assertThat(response.categories()).hasSize(4);

        assertThat(response.categories().get(0).id()).isEqualTo("cafe");
        assertThat(response.categories().get(0).label()).isEqualTo("Кафе и рестораны");
        assertThat(response.categories().get(0).percent()).isEqualTo("5%");
        assertThat(response.categories().get(0).tone()).isEqualTo("green");

        assertThat(response.categories().get(1).id()).isEqualTo("azs");
        assertThat(response.categories().get(1).label()).isEqualTo("АЗС");
        assertThat(response.categories().get(1).percent()).isEqualTo("3%");
        assertThat(response.categories().get(1).tone()).isEqualTo("green");

        assertThat(response.categories().get(2).id()).isEqualTo("pharmacy");
        assertThat(response.categories().get(2).label()).isEqualTo("Аптеки");
        assertThat(response.categories().get(2).percent()).isEqualTo("7%");
        assertThat(response.categories().get(2).tone()).isEqualTo("green");

        assertThat(response.categories().get(3).id()).isEqualTo("other");
        assertThat(response.categories().get(3).label()).isEqualTo("Прочее");
        assertThat(response.categories().get(3).percent()).isEqualTo("1%");
        assertThat(response.categories().get(3).tone()).isEqualTo("gray");

        assertThat(response.promo().title()).isEqualTo("Скидка 30% на связь МТС при оплате картой");
        assertThat(response.promo().text())
                .isEqualTo("Действует до 31.12.2026 · автоматически при оплате связи картой Zero");
    }
}
