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
 * Covers the complaints rows of story
 * 11-incident-case-single-incidents-complaints' I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions). See {@code SingleIncidentControllerIntegrationTest}'s
 * class Javadoc for why a Testcontainers Kafka broker is also started here
 * even though these tests never touch Kafka.
 *
 * All test methods share one Spring context and one Postgres container
 * (the {@code @Container} fields are static); {@code @TestMethodOrder} +
 * {@code @Order} pin a fixed execution order since later tests' mutations
 * are visible to earlier-numbered assertions otherwise.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComplaintControllerIntegrationTest {

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
        List<ComplaintResponse> complaints = webTestClient()
                .get().uri("/clients/client-1/complaints")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ComplaintResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(complaints).isNotNull().hasSize(2);

        // Newest-first ordering (JpaComplaintRepository: findByClientIdOrderByCreatedDateDescIdAsc) --
        // "Жалоба №118240" (2026-09-10) before "Претензия №117580" (2026-07-20).
        assertThat(complaints.get(0).id()).isEqualTo("Жалоба №118240");
        assertThat(complaints.get(1).id()).isEqualTo("Претензия №117580");

        ComplaintResponse first = complaints.stream().filter(c -> c.id().equals("Жалоба №118240")).findFirst()
                .orElseThrow();
        assertThat(first.theme()).isEqualTo("Перевод не пришёл получателю");
        assertThat(first.created()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(first.term()).isEqualTo("2 дня");
        assertThat(first.status()).isEqualTo("На 2 линии");

        ComplaintResponse second = complaints.stream().filter(c -> c.id().equals("Претензия №117580")).findFirst()
                .orElseThrow();
        assertThat(second.theme()).isEqualTo("Возврат комиссии за обслуживание");
        assertThat(second.created()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(second.term()).isEqualTo("—");
        assertThat(second.status()).isEqualTo("Удовлетворена");
    }

    @Test
    @Order(2)
    void unknownClientReturns200EmptyListNever404() {
        webTestClient()
                .get().uri("/clients/nope/complaints")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ComplaintResponse.class)
                .isEqualTo(List.of());
    }

    @Test
    @Order(3)
    void createWithValidThemeReturns201WithComplaintPrefixedId() {
        ComplaintResponse created = webTestClient()
                .post().uri("/clients/client-1/complaints")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"theme\":\"Тест\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ComplaintResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).startsWith("complaint-");
        assertThat(created.theme()).isEqualTo("Тест");
        assertThat(created.created()).isEqualTo(LocalDate.now());
        assertThat(created.term()).isEqualTo("—");
        assertThat(created.status()).isEqualTo("На 2 линии");

        List<ComplaintResponse> afterCreate = webTestClient()
                .get().uri("/clients/client-1/complaints")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ComplaintResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(afterCreate).extracting(ComplaintResponse::id).contains(created.id());
    }

    @Test
    @Order(4)
    void createWithBlankThemeReturns400WithCleanValidationMessage() {
        webTestClient()
                .post().uri("/clients/client-1/complaints")
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
                .post().uri("/clients/client-1/complaints")
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
                .put().uri("/clients/client-1/complaints")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
