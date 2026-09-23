package ru.mtsbank.cardoffice.offers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for offers-service.
 *
 * Story 0 scope: proves the container starts, connects to its own PostgreSQL
 * instance and answers on the Spring Boot Actuator health endpoint. No business
 * capability is implemented here yet -- see docs/specs/spec-mts-client-card-office-backend.
 */
@SpringBootApplication
public class OffersServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OffersServiceApplication.class, args);
    }
}
