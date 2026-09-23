package ru.mtsbank.cardoffice.gateway;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Covers the frozen I/O & Edge-Case Matrix row "Target service down -> Gateway
 * returns 502/503, not a hang" (previously only checked by a one-time manual
 * curl against a stopped docker-compose container). Adds one extra route,
 * pointed at a port nothing is listening on, alongside the application's real
 * routes (Spring Cloud Gateway combines every {@link RouteLocator} bean found
 * in the context, so this does not disturb the production route table defined
 * in application.yml), and asserts the Gateway answers with 502 and the
 * {@link GatewayErrorHandlerConfiguration} error envelope, rather than hanging
 * or surfacing a bare 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GatewayErrorHandlerConfigurationTest.TestRouteConfig.class)
class GatewayErrorHandlerConfigurationTest {

    @LocalServerPort
    private int port;

    @Test
    void downstreamConnectionFailureMapsTo502WithUpstreamUnavailableEnvelope() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();

        client.get().uri("/__test-down-backend/anything")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.error.code").isEqualTo("UPSTREAM_UNAVAILABLE")
                .jsonPath("$.error.message").exists();
    }

    @TestConfiguration
    static class TestRouteConfig {

        @Bean
        RouteLocator testDownBackendRoute(RouteLocatorBuilder builder) throws IOException {
            int closedPort;
            try (ServerSocket socket = new ServerSocket(0)) {
                closedPort = socket.getLocalPort();
            }
            String targetUri = "http://localhost:" + closedPort;
            return builder.routes()
                    .route("test-down-backend", r -> r.path("/__test-down-backend/**")
                            .filters(f -> f.stripPrefix(1))
                            .uri(targetUri))
                    .build();
        }
    }
}
