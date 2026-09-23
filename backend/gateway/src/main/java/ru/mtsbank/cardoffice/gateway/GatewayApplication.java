package ru.mtsbank.cardoffice.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for the API Gateway (ARCHITECTURE-SPINE.md AD-6): the single
 * frontend entry point and the only container in docker-compose.yml that
 * publishes a host port. Route table lives in application.yml.
 *
 * Story 0 scope: proves the route table forwards to each of the 9 services'
 * health endpoints. No business routing/auth beyond the AD-7 trusted-header
 * stub is implemented here yet.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
