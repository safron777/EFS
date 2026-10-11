package ru.mtsbank.cardoffice.notifications.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Covers all six rows of story 9-notifications' I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions), mirroring {@code CashbackControllerIntegrationTest}'s
 * shape. {@code @ServiceConnection} points Spring Boot's datasource at the
 * container, so Liquibase runs the actual changelog (001/002) on every
 * test run.
 *
 * Unlike CAP-8's purely read-only cashback fixture, CAP-9's toggle mutation
 * genuinely persists a state change against the one shared Postgres
 * instance backing every test method in this class (the Spring test
 * context -- and so the database -- is reused across methods). {@code
 * @TestMethodOrder}/{@code @Order} pins the toggle-and-verify test last so
 * it never runs before the fixture-matching read assertions, and that test
 * restores the channel to its original {@code on: true} state at the end
 * so it leaves no observable side effect for any later test run.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationsControllerIntegrationTest {

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
    void knownCardReturns200MatchingMockDataFixture() {
        NotificationsResponse response = webTestClient()
                .get().uri("/cards/card-4019/notifications")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertFixtureMatches(response);
    }

    @Test
    @Order(2)
    void differentCardReturnsIdenticalUnconditionalFixture() {
        NotificationsResponse response = webTestClient()
                .get().uri("/cards/card-7274/notifications")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertFixtureMatches(response);
    }

    @Test
    @Order(3)
    void toggleUnknownChannelReturns404WithErrorEnvelope() {
        webTestClient()
                .post().uri("/cards/card-4019/notifications/nope")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"on\": false}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(4)
    void toggleWithMissingOnFieldReturns400WithCleanValidationMessage() {
        webTestClient()
                .post().uri("/cards/card-4019/notifications/sms-ops")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(5)
    void nonPostMethodOnTogglePathReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .put().uri("/cards/card-4019/notifications/sms-ops")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(6)
    void toggleKnownChannelPersistsAndIsReflectedByASubsequentGet() {
        webTestClient()
                .post().uri("/cards/card-4019/notifications/sms-ops")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"on\": false}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);

        NotificationsResponse afterToggleOff = webTestClient()
                .get().uri("/cards/card-4019/notifications")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(afterToggleOff).isNotNull();
        assertThat(findItem(afterToggleOff, "sms-ops").on()).isFalse();

        // Restore the original fixture state so this mutation leaves no
        // observable side effect for any later test run against the same
        // shared Postgres instance (see class Javadoc).
        webTestClient()
                .post().uri("/cards/card-4019/notifications/sms-ops")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"on\": true}")
                .exchange()
                .expectStatus().isOk();

        NotificationsResponse afterRestore = webTestClient()
                .get().uri("/cards/card-4019/notifications")
                .exchange()
                .expectStatus().isOk()
                .expectBody(NotificationsResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(afterRestore).isNotNull();
        assertThat(findItem(afterRestore, "sms-ops").on()).isTrue();
    }

    private static NotificationsResponse.ItemResponse findItem(NotificationsResponse response, String id) {
        return response.items().stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void assertFixtureMatches(NotificationsResponse response) {
        assertThat(response.items()).hasSize(3);

        assertThat(response.items().get(0).id()).isEqualTo("sms-ops");
        assertThat(response.items().get(0).title()).isEqualTo("SMS по операциям");
        assertThat(response.items().get(0).subtitle()).isEqualTo("Списания и зачисления по карте •••• 4019");
        assertThat(response.items().get(0).on()).isTrue();
        assertThat(response.items().get(0).action()).isEqualTo("Отключить");

        assertThat(response.items().get(1).id()).isEqualTo("sms-plan");
        assertThat(response.items().get(1).title()).isEqualTo("SMS о плановых платежах");
        assertThat(response.items().get(1).subtitle()).isEqualTo("Напоминания за 3 дня до списания");
        assertThat(response.items().get(1).on()).isTrue();
        assertThat(response.items().get(1).action()).isEqualTo("Отключить");

        assertThat(response.items().get(2).id()).isEqualTo("push");
        assertThat(response.items().get(2).title()).isEqualTo("Push-уведомления в приложении");
        assertThat(response.items().get(2).subtitle()).isEqualTo("Операции, кэшбэк, акции");
        assertThat(response.items().get(2).on()).isTrue();
        assertThat(response.items().get(2).action()).isEqualTo("Настроить");

        assertThat(response.method().title()).isEqualTo("Способ уведомлений (ОМТ)");
        assertThat(response.method().current()).isEqualTo("Email · v***@mail.ru");
    }
}
