package ru.mtsbank.cardoffice.clientidentity.domain;

import java.util.Optional;

/**
 * Repository port: the domain's view of persistence, implemented by an
 * adapter in {@code adapters/out/persistence} (spine Design Paradigm). The
 * domain depends on this interface only, never on JPA/Spring Data directly.
 */
public interface ClientRepository {

    Optional<Client> findById(String id);
}
