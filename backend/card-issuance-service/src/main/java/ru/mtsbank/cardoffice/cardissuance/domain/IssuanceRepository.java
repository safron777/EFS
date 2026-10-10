package ru.mtsbank.cardoffice.cardissuance.domain;

import java.util.List;

/**
 * Repository port: the domain's view of issuance-history persistence,
 * implemented by an adapter in {@code adapters/out/persistence} (spine
 * Design Paradigm). The domain depends on this interface only, never on
 * JPA/Spring Data directly.
 *
 * Only {@code history} rows are persisted -- {@code CurrentApplication}/
 * {@code NewCardInfo} are static fixtures with no repository of their own
 * (story Boundaries & Constraints).
 */
public interface IssuanceRepository {

    /** Returns every history row in seeded/created order (story Boundaries & Constraints). */
    List<IssuanceApplication> findAllOrdered();

    /** Persists a new application row (the {@code POST .../issuance/applications} action) and returns it. */
    IssuanceApplication save(IssuanceApplication application);
}
