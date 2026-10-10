package ru.mtsbank.cardoffice.operations.domain;

/**
 * Repository port: the domain's view of operations-history persistence,
 * implemented by an adapter in {@code adapters/out/persistence} (spine
 * Design Paradigm). The domain depends on this interface only, never on
 * JPA/Spring Data directly.
 *
 * {@code getHistory} never throws/returns not-found for an unknown {@code
 * cardId} -- this is a collection-shaped endpoint (story Boundaries &
 * Constraints: no {@code NOT_FOUND} case), so an unmatched card simply
 * yields an {@link OperationsHistory} with a {@code null} alert and empty
 * rows, the same 200-empty precedent {@code CardRepository} set for an
 * unmatched {@code clientId}/{@code productId}.
 */
public interface OperationsRepository {

    OperationsHistory getHistory(String cardId);
}
