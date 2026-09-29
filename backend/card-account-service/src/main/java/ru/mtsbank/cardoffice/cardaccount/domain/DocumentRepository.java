package ru.mtsbank.cardoffice.cardaccount.domain;

import java.util.List;

/**
 * Repository port: the domain's view of document persistence, implemented
 * by an adapter in {@code adapters/out/persistence} (spine Design
 * Paradigm). The domain depends on this interface only, never on JPA/Spring
 * Data directly.
 *
 * {@code existsById} backs the {@code send}/{@code print} use cases' 404
 * check (story Boundaries & Constraints: unknown {@code documentId} is a
 * real not-found case, unlike {@code CardRepository}'s collection-only
 * query) without pulling a full {@link Document} just to check presence.
 */
public interface DocumentRepository {

    List<Document> findAll();

    boolean existsById(String id);
}
