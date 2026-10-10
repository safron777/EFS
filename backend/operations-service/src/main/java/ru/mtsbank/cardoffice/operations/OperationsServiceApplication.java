package ru.mtsbank.cardoffice.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for operations-service.
 *
 * Story 6-operations (CAP-6) adds the first business capability: {@code GET
 * /cards/{cardId}/operations} -- see
 * docs/specs/spec-mts-client-card-office-backend.
 */
@SpringBootApplication
public class OperationsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationsServiceApplication.class, args);
    }
}
