package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

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
import org.testcontainers.kafka.KafkaContainer;

/**
 * Covers the single-incidents rows of story
 * 11-incident-case-single-incidents-complaints' I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions). {@code @ServiceConnection} points Spring Boot's datasource
 * at the container, so Liquibase runs the actual changelog (001-003:
 * mass-incidents schema, single-incidents/complaints schema, seed) on
 * every test run.
 *
 * A Testcontainers Kafka broker is also started here, same rationale as
 * {@code MassIncidentControllerIntegrationTest}: this service's Spring
 * context always wires {@code @KafkaListener} containers and a {@code
 * KafkaAdmin} (CAP-10), so without a reachable broker the context would
 * spend time retrying against an unreachable {@code kafka:9092} at
 * startup, even though these tests never touch Kafka themselves.
 *
 * All test methods share one Spring context and one Postgres container
 * (the {@code @Container} fields are static), so mutations from one method
 * are visible to methods that run after it -- {@code @TestMethodOrder} +
 * {@code @Order} pin a fixed execution order for that reason, same
 * precedent as {@code IssuanceControllerIntegrationTest}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingleIncidentControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @Order(1)
    void knownClientReturns200WithTwoSeededRowsMatchingMockExactly() {
        List<SingleIncidentResponse> incidents = webTestClient()
                .get().uri("/clients/client-1/incidents/single")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SingleIncidentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(incidents).isNotNull().hasSize(2);

        // Newest-first ordering (JpaSingleIncidentRepository: findByClientIdOrderByCreatedDateDescIdAsc) --
        // ЕИ-118225 (2026-09-02) before ЕИ-117904 (2026-08-14).
        assertThat(incidents.get(0).id()).isEqualTo("ЕИ-118225");
        assertThat(incidents.get(1).id()).isEqualTo("ЕИ-117904");

        SingleIncidentResponse first = incidents.stream().filter(i -> i.id().equals("ЕИ-118225")).findFirst()
                .orElseThrow();
        assertThat(first.theme()).isEqualTo("Некорректный баланс по карте");
        assertThat(first.created()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(first.line()).isEqualTo("2 линия");
        assertThat(first.status()).isEqualTo("В работе");

        SingleIncidentResponse second = incidents.stream().filter(i -> i.id().equals("ЕИ-117904")).findFirst()
                .orElseThrow();
        assertThat(second.theme()).isEqualTo("Ошибка активации карты");
        assertThat(second.created()).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(second.line()).isEqualTo("2 линия");
        assertThat(second.status()).isEqualTo("Решён");
    }

    @Test
    @Order(2)
    void unknownClientReturns200EmptyListNever404() {
        webTestClient()
                .get().uri("/clients/nope/incidents/single")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SingleIncidentResponse.class)
                .isEqualTo(List.of());
    }

    @Test
    @Order(3)
    void createWithValidThemeReturns201AndAppearsInSubsequentGet() {
        SingleIncidentResponse created = webTestClient()
                .post().uri("/clients/client-1/incidents/single")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"theme\":\"Тест\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SingleIncidentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).startsWith("ЕИ-");
        // Visibly distinct from the seeded rows' own numbers (story Boundaries & Constraints).
        assertThat(created.id()).isNotIn("ЕИ-118225", "ЕИ-117904");
        assertThat(created.theme()).isEqualTo("Тест");
        assertThat(created.created()).isEqualTo(LocalDate.now());
        assertThat(created.line()).isEqualTo("2 линия");
        assertThat(created.status()).isEqualTo("В работе");

        List<SingleIncidentResponse> afterCreate = webTestClient()
                .get().uri("/clients/client-1/incidents/single")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SingleIncidentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(afterCreate).extracting(SingleIncidentResponse::id).contains(created.id());
    }

    @Test
    @Order(4)
    void createWithBlankThemeReturns400WithCleanValidationMessage() {
        webTestClient()
                .post().uri("/clients/client-1/incidents/single")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"theme\":\"\"}")
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(5)
    void createWithThemeOverSizeLimitReturns400WithCleanValidationMessage() {
        String overLimitTheme = "а".repeat(256);
        webTestClient()
                .post().uri("/clients/client-1/incidents/single")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"theme\":\"" + overLimitTheme + "\"}")
                .exchange()
                .expectStatus().isEqualTo(400)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(6)
    void nonPostMethodOnCreatePathReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .put().uri("/clients/client-1/incidents/single")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
