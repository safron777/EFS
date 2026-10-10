package ru.mtsbank.cardoffice.cashback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point for cashback-service.
 *
 * CAP-8 (cashback): {@code GET /cards/{cardId}/cashback}, backed by its own
 * PostgreSQL schema via Liquibase (AD-12) -- see
 * docs/specs/spec-mts-client-card-office-backend/stories/8-cashback.md.
 */
@SpringBootApplication
public class CashbackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashbackServiceApplication.class, args);
    }
}
