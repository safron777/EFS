package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Covers all three rows of story 3-card-account-cards' I/O & Edge-Case
 * Matrix against a real, migrated Postgres (Testcontainers, per spine
 * Consistency Conventions: "each service's integration tests run against
 * its own Testcontainers instances"). {@code @ServiceConnection} points
 * Spring Boot's datasource at the container, so Liquibase runs the actual
 * changelog (schema + seed) on every test run.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CardControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownClientAndProductReturnsBothCardsWithKopecksAndIsoDates() {
        List<CardResponse> cards = webTestClient()
                .get().uri("/clients/client-1/products/debit-mts-dengi/cards")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CardResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(cards).isNotNull();
        assertThat(cards).hasSize(2);

        CardResponse main = cards.get(0);
        assertThat(main.id()).isEqualTo("card-4019");
        assertThat(main.product()).isEqualTo("Zero");
        assertThat(main.masked()).isEqualTo("•••• 4019");
        assertThat(main.subtitle()).isEqualTo("8_MC World Zero RUR");
        assertThat(main.isMain()).isTrue();
        assertThat(main.status()).isEqualTo("active");
        // AD-3: the mock's raw-ruble 12430 corrected to kopecks.
        assertThat(main.balance()).isEqualTo(1_243_000L);
        // ISO-8601, not the mock's "12.01.2025".
        assertThat(main.issuedAt()).isEqualTo(LocalDate.of(2025, 1, 12));
        assertThat(main.account()).isEqualTo("****6272");
        assertThat(main.closedAt()).isNull();

        CardResponse closed = cards.get(1);
        assertThat(closed.id()).isEqualTo("card-7274");
        assertThat(closed.product()).isEqualTo("ZERO");
        assertThat(closed.masked()).isEqualTo("•••• 7274");
        assertThat(closed.subtitle()).isEqualTo("8_MC World Zero RUR · Виртуальная");
        assertThat(closed.isMain()).isFalse();
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.balance()).isEqualTo(0L);
        assertThat(closed.issuedAt()).isNull();
        assertThat(closed.account()).isNull();
        assertThat(closed.closedAt()).isEqualTo(LocalDate.of(2025, 10, 20));
    }

    @Test
    void perRowFieldPresenceMatchesMockExactlyNotPaddedWithNulls() {
        String body = webTestClient()
                .get().uri("/clients/client-1/products/debit-mts-dengi/cards")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).isNotNull();
        // @JsonInclude(NON_NULL) omits absent fields entirely -- parse the
        // JSON structurally and assert the key is genuinely absent (not
        // present-with-null) on the row that lacks it. A string/substring
        // check here would be fragile against pretty-printing, key
        // reordering, or a value that happens to contain "},{".
        JsonNode rows = new ObjectMapper().readTree(body);
        assertThat(rows.get(0).has("closedAt")).isFalse();
        assertThat(rows.get(1).has("account")).isFalse();
        assertThat(rows.get(1).has("issuedAt")).isFalse();
    }

    @Test
    void unknownClientOrProductReturns200WithEmptyArray() {
        webTestClient()
                .get().uri("/clients/nope/products/debit-mts-dengi/cards")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CardResponse.class)
                .hasSize(0);
    }

    @Test
    void nonGetMethodReturns405WithErrorEnvelopeNot500() {
        webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
