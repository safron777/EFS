package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers all three rows of story 5-card-issuance's I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions). {@code @ServiceConnection} points Spring Boot's datasource
 * at the container, so Liquibase runs the actual changelog (schema + seed)
 * on every test run.
 *
 * All test methods share one Spring context and one Postgres container
 * (the {@code @Container} field is static), so mutations from one method
 * are visible to methods that run after it. {@code @TestMethodOrder} +
 * {@code @Order} pin a fixed execution order for that reason: the GET
 * test's "exactly 3 history rows" assertion only holds if it runs before
 * any test that creates a new application.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IssuanceControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @Order(1)
    void getIssuanceReturns200MatchingMockDataExactlyWithIsoDatesAndSameRowOrder() {
        IssuanceResponse response = webTestClient()
                .get().uri("/clients/client-1/products/debit-mts-dengi/issuance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(IssuanceResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        assertThat(response.currentApplication().card()).isEqualTo("Zero");
        assertThat(response.currentApplication().status()).isEqualTo("Изготовлена");
        assertThat(response.currentApplication().text())
                .isEqualTo("Способ получения: курьер · ожидаемая дата 22.09.2026");

        assertThat(response.newCard().text()).isEqualTo("Виртуальная — мгновенно · пластиковая — 3–5 дней");

        List<ApplicationResponse> history = response.history();
        assertThat(history).hasSize(3);

        // Seeded order preserved exactly -- app1 (newest) first, not date-sorted.
        assertThat(history.get(0).id()).isEqualTo("app1");
        assertThat(history.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(history.get(0).card()).isEqualTo("Zero (пластик)");
        assertThat(history.get(0).method()).isEqualTo("Курьер");
        assertThat(history.get(0).status()).isEqualTo("Изготовлена");

        assertThat(history.get(1).id()).isEqualTo("app2");
        assertThat(history.get(1).date()).isEqualTo(LocalDate.of(2025, 1, 12));
        assertThat(history.get(1).card()).isEqualTo("Zero (виртуальная)");
        assertThat(history.get(1).method()).isEqualTo("Мгновенно");
        assertThat(history.get(1).status()).isEqualTo("Выдана");

        assertThat(history.get(2).id()).isEqualTo("app3");
        assertThat(history.get(2).date()).isEqualTo(LocalDate.of(2025, 10, 20));
        assertThat(history.get(2).card()).isEqualTo("ZERO (виртуальная)");
        assertThat(history.get(2).method()).isEqualTo("Мгновенно");
        assertThat(history.get(2).status()).isEqualTo("Закрыта");
    }

    @Test
    @Order(2)
    void createApplicationWithValidBodyReturns201AndAppearsAppendedInSubsequentGet() {
        ApplicationResponse created = webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/issuance/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"card\":\"Zero\",\"method\":\"Курьер\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotBlank();
        // Visibly different provenance from the seeded "appN" ids (a generated UUID).
        assertThat(created.id()).doesNotStartWith("app");
        assertThat(created.date()).isEqualTo(LocalDate.now());
        assertThat(created.card()).isEqualTo("Zero");
        assertThat(created.method()).isEqualTo("Курьер");
        assertThat(created.status()).isEqualTo("В обработке");

        IssuanceResponse afterCreate = webTestClient()
                .get().uri("/clients/client-1/products/debit-mts-dengi/issuance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(IssuanceResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(afterCreate).isNotNull();
        assertThat(afterCreate.history()).hasSizeGreaterThanOrEqualTo(4);
        // Appended after the three seeded rows, not re-sorted or inserted elsewhere.
        ApplicationResponse last = afterCreate.history().get(afterCreate.history().size() - 1);
        assertThat(last.id()).isEqualTo(created.id());

        // currentApplication/newCard are untouched by the POST (story Boundaries & Constraints).
        assertThat(afterCreate.currentApplication().card()).isEqualTo("Zero");
        assertThat(afterCreate.currentApplication().status()).isEqualTo("Изготовлена");
        assertThat(afterCreate.newCard().text()).isEqualTo("Виртуальная — мгновенно · пластиковая — 3–5 дней");
    }

    @Test
    @Order(3)
    void createTwoApplicationsSequentiallyLandWithDistinctIncreasingOrder() {
        ApplicationResponse first = createApplication("Zero", "Курьер");
        ApplicationResponse second = createApplication("ZERO", "Мгновенно");

        assertThat(first.id()).isNotEqualTo(second.id());

        List<ApplicationResponse> history = webTestClient()
                .get().uri("/clients/client-1/products/debit-mts-dengi/issuance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(IssuanceResponse.class)
                .returnResult()
                .getResponseBody()
                .history();

        int firstIndex = indexOfId(history, first.id());
        int secondIndex = indexOfId(history, second.id());

        assertThat(firstIndex).isGreaterThanOrEqualTo(0);
        // Each sequential create gets the next sort_order, immediately after the
        // previous one -- distinct and strictly increasing, with no gap or reordering.
        assertThat(secondIndex).isEqualTo(firstIndex + 1);
    }

    private ApplicationResponse createApplication(String card, String method) {
        return webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/issuance/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"card\":\"" + card + "\",\"method\":\"" + method + "\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private static int indexOfId(List<ApplicationResponse> history, String id) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Test
    @Order(4)
    void createApplicationWithMissingFieldReturns400WithValidationErrorEnvelope() {
        webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/issuance/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"card\":\"Zero\"}")
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(5)
    void createApplicationWithBlankFieldReturns400WithValidationErrorEnvelope() {
        webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/issuance/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"card\":\"Zero\",\"method\":\"   \"}")
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(6)
    void nonGetMethodOnIssuanceReturns405WithErrorEnvelopeNot500() {
        webTestClient()
                .post().uri("/clients/client-1/products/debit-mts-dengi/issuance")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
