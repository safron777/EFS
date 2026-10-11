package ru.mtsbank.cardoffice.incidentcase.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import ru.mtsbank.cardoffice.incidentcase.adapters.in.web.MassIncidentResponse;

/**
 * Covers story 10-incident-case-mass-incidents' Kafka-facing I/O & Edge-Case
 * Matrix rows against a real Testcontainers Postgres and a real
 * Testcontainers Kafka broker (story Code Map/Tasks: "a real
 * embedded/Testcontainers Kafka broker"), mirroring
 * {@code NotificationsControllerIntegrationTest}'s Testcontainers shape.
 * {@code @ServiceConnection} on both containers points Spring Boot's
 * datasource and {@code spring.kafka.bootstrap-servers} at them, so
 * Liquibase runs the real changelog and the three {@code @KafkaListener}
 * methods consume from the real broker.
 *
 * Every test publishes via the application's own {@code KafkaTemplate}, the
 * same producer path a real monitoring system would use, and polls {@code
 * GET /incidents/mass} with Awaitility rather than a fixed sleep, since
 * consumption is asynchronous. {@code @TestMethodOrder}/{@code @Order} pins
 * the very-first "nothing consumed yet" assertion before any publish, the
 * same precedent as {@code NotificationsControllerIntegrationTest}; later
 * tests each use their own unique incident id so they don't interfere with
 * each other despite sharing one broker/database across the class.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MassIncidentEventListenerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @Container
    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.3.1");

    private static org.apache.kafka.clients.consumer.Consumer<String, String> deadLetterConsumer;

    @LocalServerPort
    private int port;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeAll
    static void subscribeToDeadLetterTopic() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dead-letter-test-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        deadLetterConsumer = new KafkaConsumer<>(props);
        deadLetterConsumer.subscribe(List.of(KafkaTopicsConfig.TOPIC_DEAD_LETTER));
    }

    @AfterAll
    static void closeDeadLetterConsumer() {
        if (deadLetterConsumer != null) {
            deadLetterConsumer.close();
        }
    }

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private void publish(String topic, String payload) throws Exception {
        kafkaTemplate.send(topic, payload).get(10, TimeUnit.SECONDS);
    }

    private List<MassIncidentResponse> getMassIncidents() {
        return webTestClient()
                .get().uri("/incidents/mass")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MassIncidentResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private MassIncidentResponse findIncident(String id) {
        return getMassIncidents().stream().filter(i -> i.id().equals(id)).findFirst().orElse(null);
    }

    @Test
    @Order(1)
    void noIncidentsConsumedYetReturnsEmptyList() {
        List<MassIncidentResponse> incidents = getMassIncidents();
        assertThat(incidents).isNotNull();
        // Other tests in this class run after this one and publish their own
        // incidents, but nothing has been published yet at this point in the
        // ordered sequence.
        assertThat(incidents).isEmpty();
    }

    @Test
    @Order(2)
    void openedEventIsConsumedAndAppearsInReadModelWithMatchesClientFalse() throws Exception {
        String id = "MI-TEST-OPEN";
        publish(KafkaTopicsConfig.TOPIC_OPENED, openedEnvelope(id, "evt-open-1", "В работе", "~3 200 клиентов"));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            MassIncidentResponse incident = findIncident(id);
            assertThat(incident).isNotNull();
            assertThat(incident.theme()).isEqualTo("Отклонения переводов по СБП");
            assertThat(incident.product()).isEqualTo("Дебетовые карты МТС Деньги");
            assertThat(incident.start()).isEqualTo(java.time.LocalDate.of(2026, 9, 12));
            assertThat(incident.status()).isEqualTo("В работе");
            assertThat(incident.affected()).isEqualTo("~3 200 клиентов");
            assertThat(incident.matchesClient()).isFalse();
        });
    }

    @Test
    @Order(3)
    void reDeliveredOpenedEventForSameIdIsIdempotentUpsert() throws Exception {
        String id = "MI-TEST-OPEN";
        // Same id as the previous test's opened event -- Kafka's at-least-once
        // delivery makes a re-delivered opened event for an already-known id a
        // normal, reachable case (story Boundaries & Constraints): it must not
        // crash the consumer or be rejected, just upsert again.
        publish(KafkaTopicsConfig.TOPIC_OPENED, openedEnvelope(id, "evt-open-2", "В работе", "~3 200 клиентов"));

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<MassIncidentResponse> incidents = getMassIncidents();
            assertThat(incidents).filteredOn(i -> i.id().equals(id)).hasSize(1);
        });
    }

    @Test
    @Order(4)
    void updatedEventChangesOnlyStatusAndAffected() throws Exception {
        String id = "MI-TEST-OPEN";
        String updated =
                """
                {"eventId":"evt-update-1","eventType":"MassIncidentUpdated","occurredAt":"2026-09-13T08:00:00Z","version":1,\
                "data":{"id":"%s","status":"Устраняется","affected":"~4 000 клиентов"}}\
                """
                        .formatted(id);
        publish(KafkaTopicsConfig.TOPIC_UPDATED, updated);

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            MassIncidentResponse incident = findIncident(id);
            assertThat(incident).isNotNull();
            assertThat(incident.status()).isEqualTo("Устраняется");
            assertThat(incident.affected()).isEqualTo("~4 000 клиентов");
            // theme/product/start stay exactly what opened fixed.
            assertThat(incident.theme()).isEqualTo("Отклонения переводов по СБП");
            assertThat(incident.product()).isEqualTo("Дебетовые карты МТС Деньги");
            assertThat(incident.start()).isEqualTo(java.time.LocalDate.of(2026, 9, 12));
        });
    }

    @Test
    @Order(5)
    void updatedEventForUnknownIncidentIsLoggedAndSkippedWithoutCrashingConsumer() throws Exception {
        String updated =
                """
                {"eventId":"evt-update-unknown","eventType":"MassIncidentUpdated","occurredAt":"2026-09-13T08:00:00Z",\
                "version":1,"data":{"id":"MI-TEST-UNKNOWN","status":"Устраняется","affected":"~1 клиент"}}\
                """;
        publish(KafkaTopicsConfig.TOPIC_UPDATED, updated);

        // Nothing to assert in the read model (the id was never opened); the real
        // assertion is that this never appears and the consumer keeps running --
        // proven by the next ordered test's successful publish/consume.
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(findIncident("MI-TEST-UNKNOWN")).isNull());
    }

    @Test
    @Order(6)
    void closedEventChangesStatus() throws Exception {
        String id = "MI-TEST-OPEN";
        String closed =
                """
                {"eventId":"evt-close-1","eventType":"MassIncidentClosed","occurredAt":"2026-09-14T09:00:00Z","version":1,\
                "data":{"id":"%s","status":"Устранён"}}\
                """
                        .formatted(id);
        publish(KafkaTopicsConfig.TOPIC_CLOSED, closed);

        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            MassIncidentResponse incident = findIncident(id);
            assertThat(incident).isNotNull();
            assertThat(incident.status()).isEqualTo("Устранён");
        });
    }

    @Test
    @Order(7)
    void closedEventForUnknownIncidentIsLoggedAndSkippedWithoutCrashingConsumer() throws Exception {
        String closed =
                """
                {"eventId":"evt-close-unknown","eventType":"MassIncidentClosed","occurredAt":"2026-09-14T09:00:00Z",\
                "version":1,"data":{"id":"MI-TEST-UNKNOWN-2","status":"Устранён"}}\
                """;
        publish(KafkaTopicsConfig.TOPIC_CLOSED, closed);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(findIncident("MI-TEST-UNKNOWN-2")).isNull());
    }

    @Test
    @Order(8)
    void unsupportedVersionEventIsNeverAppliedAndRoutedToDeadLetterVerbatim() throws Exception {
        String id = "MI-TEST-BAD-VERSION";
        String eventId = "evt-bad-version-1";
        String badVersionEnvelope =
                """
                {"eventId":"%s","eventType":"MassIncidentOpened","occurredAt":"2026-09-15T10:00:00Z","version":99,\
                "data":{"id":"%s","theme":"Тест","product":"Тест","start":"2026-09-15","status":"В работе",\
                "affected":"~1 клиент"}}\
                """
                        .formatted(eventId, id);
        publish(KafkaTopicsConfig.TOPIC_OPENED, badVersionEnvelope);

        // Never applied to the read model.
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(findIncident(id)).isNull());

        // Appears on the dead-letter topic, byte-for-byte identical to what was published.
        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(deadLetterConsumer, KafkaTopicsConfig.TOPIC_DEAD_LETTER, Duration.ofSeconds(20));
        assertThat(record.value()).isEqualTo(badVersionEnvelope);
    }

    @Test
    @Order(9)
    void dataConversionFailureIsRoutedToDeadLetterAndConsumerKeepsRunning() throws Exception {
        String id = "MI-TEST-BAD-DATA";
        String eventId = "evt-bad-data-1";
        // version is supported (1) and the envelope itself parses fine, but "start" isn't a
        // valid date, so converting data to MassIncidentOpenedData throws inside the handler.
        String badDataEnvelope =
                """
                {"eventId":"%s","eventType":"MassIncidentOpened","occurredAt":"2026-09-16T10:00:00Z","version":1,\
                "data":{"id":"%s","theme":"Тест","product":"Тест","start":"not-a-date","status":"В работе",\
                "affected":"~1 клиент"}}\
                """
                        .formatted(eventId, id);
        publish(KafkaTopicsConfig.TOPIC_OPENED, badDataEnvelope);

        // Never applied to the read model.
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(findIncident(id)).isNull());

        // Appears on the dead-letter topic, byte-for-byte identical to what was published.
        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(deadLetterConsumer, KafkaTopicsConfig.TOPIC_DEAD_LETTER, Duration.ofSeconds(20));
        assertThat(record.value()).isEqualTo(badDataEnvelope);

        // The consumer itself survived the exception and keeps consuming: a subsequent valid
        // event on the same topic is still picked up normally.
        String recoveryId = "MI-TEST-AFTER-BAD-DATA";
        publish(KafkaTopicsConfig.TOPIC_OPENED, openedEnvelope(recoveryId, "evt-after-bad-data", "В работе", "~1 клиент"));
        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(findIncident(recoveryId)).isNotNull());
    }

    private static String openedEnvelope(String id, String eventId, String status, String affected) {
        return """
                {"eventId":"%s","eventType":"MassIncidentOpened","occurredAt":"2026-09-12T07:00:00Z","version":1,\
                "data":{"id":"%s","theme":"Отклонения переводов по СБП","product":"Дебетовые карты МТС Деньги",\
                "start":"2026-09-12","status":"%s","affected":"%s"}}\
                """
                .formatted(eventId, id, status, affected);
    }
}
