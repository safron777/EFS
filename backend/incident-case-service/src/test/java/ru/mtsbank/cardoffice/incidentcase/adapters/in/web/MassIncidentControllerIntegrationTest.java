package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging.KafkaTopicsConfig;

/**
 * Covers the REST-only rows of story 10-incident-case-mass-incidents' I/O &
 * Edge-Case Matrix (empty list, attach known/unknown, wrong method) against
 * a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions), mirroring {@code NotificationsControllerIntegrationTest}'s
 * shape.
 *
 * A Testcontainers Kafka broker is also started here (even though these
 * tests never publish to it) purely so {@code KafkaAdmin}'s topic creation
 * and the three {@code @KafkaListener} containers have a real broker to
 * reach at context startup -- without it, Spring Boot's Kafka
 * autoconfiguration would otherwise spend the default admin operation
 * timeout retrying against an unreachable {@code kafka:9092} before giving
 * up (non-fatal, since {@code spring.kafka.admin.fail-fast} defaults to
 * {@code false}, but needlessly slow).
 *
 * The one incident this class needs already attached/known comes from
 * publishing a real {@code opened} event through the application's own
 * {@code KafkaTemplate} in {@code @Order(1)}, then waiting for it to land,
 * rather than inserting a row directly -- keeping this test honest about
 * CAP-10's actual boundary: the read model is reachable only through Kafka
 * (AD-5), never seeded directly.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MassIncidentControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    private static final String KNOWN_INCIDENT_ID = "MI-TEST-ATTACH";

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @Order(1)
    void noIncidentsConsumedYetReturns200EmptyList() throws Exception {
        List<MassIncidentResponse> incidents = webTestClient()
                .get().uri("/incidents/mass")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MassIncidentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(incidents).isNotNull().isEmpty();

        // Publish the one incident this class's attach tests need known, through
        // the real Kafka path (AD-5) -- never seeded directly into the database.
        String envelope =
                """
                {"eventId":"evt-attach-setup","eventType":"MassIncidentOpened","occurredAt":"2026-09-12T07:00:00Z",\
                "version":1,"data":{"id":"%s","theme":"Отклонения переводов по СБП",\
                "product":"Дебетовые карты МТС Деньги","start":"2026-09-12","status":"В работе",\
                "affected":"~3 200 клиентов"}}\
                """
                        .formatted(KNOWN_INCIDENT_ID);
        kafkaTemplate.send(KafkaTopicsConfig.TOPIC_OPENED, envelope).get(10, TimeUnit.SECONDS);

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<MassIncidentResponse> afterPublish = webTestClient()
                    .get().uri("/incidents/mass")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(MassIncidentResponse.class)
                    .returnResult()
                    .getResponseBody();
            assertThat(afterPublish).extracting(MassIncidentResponse::id).contains(KNOWN_INCIDENT_ID);
        });
    }

    @Test
    @Order(2)
    void knownIncidentAppearsWithMatchesClientFalseAndIsoStart() {
        MassIncidentResponse incident = webTestClient()
                .get().uri("/incidents/mass")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MassIncidentResponse.class)
                .returnResult()
                .getResponseBody()
                .stream()
                .filter(i -> i.id().equals(KNOWN_INCIDENT_ID))
                .findFirst()
                .orElseThrow();

        assertThat(incident.theme()).isEqualTo("Отклонения переводов по СБП");
        assertThat(incident.product()).isEqualTo("Дебетовые карты МТС Деньги");
        assertThat(incident.start()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(incident.status()).isEqualTo("В работе");
        assertThat(incident.affected()).isEqualTo("~3 200 клиентов");
        assertThat(incident.matchesClient()).isFalse();
    }

    @Test
    @Order(3)
    void attachKnownIncidentTwiceReturns200BothTimesAndStaysIdempotent() {
        webTestClient()
                .post().uri("/incidents/mass/" + KNOWN_INCIDENT_ID + "/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"client-1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);

        webTestClient()
                .post().uri("/incidents/mass/" + KNOWN_INCIDENT_ID + "/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"client-1\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);
    }

    @Test
    @Order(4)
    void attachUnknownIncidentReturns404WithErrorEnvelope() {
        webTestClient()
                .post().uri("/incidents/mass/nope/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"client-1\"}")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(5)
    void attachWithBlankClientIdReturns400WithCleanValidationMessage() {
        webTestClient()
                .post().uri("/incidents/mass/" + KNOWN_INCIDENT_ID + "/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"clientId\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    @Order(6)
    void nonPostMethodOnAttachPathReturns405WithErrorEnvelopeAndAllowHeader() {
        webTestClient()
                .put().uri("/incidents/mass/" + KNOWN_INCIDENT_ID + "/attach")
                .exchange()
                .expectStatus().isEqualTo(405)
                .expectHeader().exists("Allow")
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("VALIDATION_ERROR")
                .jsonPath("$.error.message").isNotEmpty();
    }
}
