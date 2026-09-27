package ru.mtsbank.cardoffice.cardaccount.domain;

import java.util.List;

/**
 * Repository port: the domain's view of persistence, implemented by an
 * adapter in {@code adapters/out/persistence} (spine Design Paradigm). The
 * domain depends on this interface only, never on JPA/Spring Data directly.
 *
 * An unmatched {@code clientId}/{@code productId} combination is not an
 * error -- this is a collection endpoint (story Boundaries & Constraints: no
 * {@code NOT_FOUND} case), so an empty list is a valid, ordinary result.
 */
public interface CardRepository {

    List<Card> findByClientIdAndProductId(String clientId, String productId);
}
