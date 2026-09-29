package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Covers all four rows of story 4-card-account-documents' I/O & Edge-Case
 * Matrix against a real, migrated Postgres (Testcontainers, per spine
 * Consistency Conventions), mirroring {@code CardControllerIntegrationTest}'s
 * shape. {@code @ServiceConnection} points Spring Boot's datasource at the
 * container, so Liquibase runs the actual changelog (001/002/003) on every
 * test run -- proving the new 003 changeset doesn't collide with or
 * invalidate 001/002.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentControllerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void listDocumentsReturns200WithAllThreeMatchingMockDataExactly() {
        List<DocumentResponse> documents = webTestClient()
                .get().uri("/clients/client-1/cards/card-4019/documents")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DocumentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(documents).isNotNull();
        assertThat(documents).hasSize(3);

        assertThat(documents.get(0).id()).isEqualTo("doc-statement");
        assertThat(documents.get(0).title()).isEqualTo("Выписка по счёту •••• 4019");
        assertThat(documents.get(0).subtitle()).isEqualTo("За последние 30 дней · PDF");

        assertThat(documents.get(1).id()).isEqualTo("doc-balance-cert");
        assertThat(documents.get(1).title()).isEqualTo("Справка об остатке денежных средств");
        assertThat(documents.get(1).subtitle()).isEqualTo("На сегодняшнюю дату");

        assertThat(documents.get(2).id()).isEqualTo("doc-flow-cert");
        assertThat(documents.get(2).title()).isEqualTo("Справка о движении средств");
        assertThat(documents.get(2).subtitle()).isEqualTo("За произвольный период, для налоговой/визы");
    }

    @Test
    void listDocumentsIgnoresUnrelatedOrNonexistentClientAndCardReturningTheSameThreeDocuments() {
        // Documents are global in this demo (story Boundaries & Constraints):
        // an unrelated/nonexistent clientId/cardId in the path is accepted at
        // face value and never cross-checked, so the response is identical
        // to the known-client-and-card case.
        List<DocumentResponse> documents = webTestClient()
                .get().uri("/clients/nope/cards/nope/documents")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DocumentResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(documents).isNotNull();
        assertThat(documents).hasSize(3);
        assertThat(documents.get(0).id()).isEqualTo("doc-statement");
        assertThat(documents.get(1).id()).isEqualTo("doc-balance-cert");
        assertThat(documents.get(2).id()).isEqualTo("doc-flow-cert");
    }

    @Test
    void sendKnownDocumentReturns200WithOkTrue() {
        webTestClient()
                .post().uri("/clients/client-1/documents/doc-statement/send")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);
    }

    @Test
    void printKnownDocumentReturns200WithOkTrue() {
        webTestClient()
                .post().uri("/clients/client-1/documents/doc-statement/print")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);
    }

    @Test
    void sendUnknownDocumentReturns404WithErrorEnvelope() {
        webTestClient()
                .post().uri("/clients/client-1/documents/nope/send")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    void printUnknownDocumentReturns404WithErrorEnvelope() {
        webTestClient()
                .post().uri("/clients/client-1/documents/nope/print")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("NOT_FOUND")
                .jsonPath("$.error.message").isNotEmpty();
    }

    @Test
    void sendWithOptionalChannelBodyIsAcceptedWithoutValidation() {
        webTestClient()
                .post().uri("/clients/client-1/documents/doc-statement/send")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"channel\":\"sms\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ok").isEqualTo(true);
    }
}
