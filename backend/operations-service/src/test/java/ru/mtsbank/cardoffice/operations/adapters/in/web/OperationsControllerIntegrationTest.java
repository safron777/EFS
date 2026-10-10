package ru.mtsbank.cardoffice.operations.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers all four rows of story 6-operations' I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions), mirroring {@code DocumentControllerIntegrationTest}'s
 * shape. {@code @ServiceConnection} points Spring Boot's datasource at the
 * container, so Liquibase runs the actual changelog (001/002) on every
 * test run.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperationsControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownCardWithHistoryReturns200WithAlertAndFiveRowsNewestFirst() {
        OperationsResponse response = webTestClient()
                .get().uri("/cards/card-4019/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBody(OperationsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();

        assertThat(response.failedAlert()).isNotNull();
        assertThat(response.failedAlert().title()).isEqualTo("Ошибка при переводе по СБП");
        assertThat(response.failedAlert().text())
                .isEqualTo("12.09.2026 · Перевод на +7 999 ХХХ-ХХ-01 · 5 000 ₽ · отклонён банком-получателем");

        List<OperationsResponse.RowResponse> rows = response.rows();
        assertThat(rows).hasSize(5);

        assertThat(rows.get(0).id()).isEqualTo("op1");
        assertThat(rows.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(rows.get(0).op()).isEqualTo("Перевод по СБП на +7 999 ХХХ-ХХ-01");
        assertThat(rows.get(0).sum()).isEqualTo(-500000L);
        assertThat(rows.get(0).status()).isEqualTo("Отклонён");

        assertThat(rows.get(1).id()).isEqualTo("op2");
        assertThat(rows.get(1).date()).isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(rows.get(1).op()).isEqualTo("Оплата · Wildberries");
        assertThat(rows.get(1).sum()).isEqualTo(-124000L);
        assertThat(rows.get(1).status()).isEqualTo("Проведена");

        assertThat(rows.get(2).id()).isEqualTo("op3");
        assertThat(rows.get(2).date()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(rows.get(2).op()).isEqualTo("Перевод другу по номеру телефона");
        assertThat(rows.get(2).sum()).isEqualTo(-200000L);
        assertThat(rows.get(2).status()).isEqualTo("Проведена");

        assertThat(rows.get(3).id()).isEqualTo("op4");
        assertThat(rows.get(3).date()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(rows.get(3).op()).isEqualTo("Оплата в приложении МТС Деньги");
        assertThat(rows.get(3).sum()).isEqualTo(-89000L);
        assertThat(rows.get(3).status()).isEqualTo("В обработке");

        assertThat(rows.get(4).id()).isEqualTo("op5");
        assertThat(rows.get(4).date()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(rows.get(4).op()).isEqualTo("Возврат · Ozon");
        assertThat(rows.get(4).sum()).isEqualTo(45000L);
        assertThat(rows.get(4).status()).isEqualTo("Проведена");
    }

    @Test
    void knownCardWithNoHistoryReturns200WithNullAlertAndEmptyRows() {
        // card-7274 is Story 3's other seeded card, but operation history is
        // inherently per-card (story Boundaries & Constraints) -- only
        // card-4019 gets fixture rows + an alert, so this is the same
        // 200-empty shape as an unknown card.
        webTestClient()
                .get().uri("/cards/card-7274/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.failedAlert").isEqualTo(null)
                .jsonPath("$.rows").isArray()
                .jsonPath("$.rows").isEmpty();
    }

    @Test
    void unknownCardReturns200WithNullAlertAndEmptyRows() {
        webTestClient()
                .get().uri("/cards/nope/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.failedAlert").isEqualTo(null)
                .jsonPath("$.rows").isArray()
                .jsonPath("$.rows").isEmpty();
    }

    @Test
    void nonGetMethodReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .post().uri("/cards/card-4019/operations")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
