package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

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
 * Covers all three rows of story 1-client-identity's I/O & Edge-Case Matrix
 * against a real, migrated Postgres (Testcontainers, per spine Consistency
 * Conventions: "each service's integration tests run against its own
 * Testcontainers instances"). {@code @ServiceConnection} points Spring Boot's
 * datasource at the container, so Liquibase runs the actual changelog on
 * every test run -- this is also the acceptance criterion "the Liquibase
 * migration actually runs on container start", exercised at the JVM level
 * rather than through docker-compose.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void knownClientReturnsFullCardMatchingMockShape() {
        ClientCardResponse response = webTestClient()
                .get().uri("/clients/client-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClientCardResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("client-1");
        assertThat(response.fullName()).isEqualTo("Христорождественская Вильгельмина А.");
        assertThat(response.clientSince()).isEqualTo(2019);
        assertThat(response.badges()).containsExactly("mts-bank", "mts-dengi");

        assertThat(response.triggers()).hasSize(5);
        assertThat(response.triggers().get(0).id()).isEqualTo("passport-expired");
        assertThat(response.triggers().get(0).link()).isEqualTo("/client/data");
        assertThat(response.triggers().get(1).id()).isEqualTo("vip");
        assertThat(response.triggers().get(1).link()).isEqualTo("/client/profile");
        assertThat(response.triggers().get(2).id()).isEqualTo("mobile-app");
        assertThat(response.triggers().get(2).link()).isNull();
        assertThat(response.triggers().get(3).id()).isEqualTo("arrests");
        assertThat(response.triggers().get(3).link()).isEqualTo("/products/debit-mts-dengi/blocks");
        assertThat(response.triggers().get(4).id()).isEqualTo("blocks");
        assertThat(response.triggers().get(4).link()).isEqualTo("/products/debit-mts-dengi/blocks");

        // openCaseIds replaces mockData.client's openCases: bare ids only (AD-2).
        assertThat(response.openCaseIds()).containsExactly("req-118391", "complaint-118240");
    }

    @Test
    void unknownClientReturns404WithErrorEnvelope() {
        webTestClient()
                .get().uri("/clients/does-not-exist")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    void triggerWithNullLinkIsSerializedAsNullNotOmitted() {
        String body = webTestClient()
                .get().uri("/clients/client-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        // The mobile-app trigger is the only one with link: null in mockData.js;
        // asserting on the raw JSON text (rather than only jsonPath, whose
        // exists()/doesNotExist() don't reliably distinguish "absent key" from
        // "present with a null value") proves the field is emitted, not dropped.
        assertThat(body).isNotNull();
        assertThat(body).contains("\"link\":null");
    }
}
