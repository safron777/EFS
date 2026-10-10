package ru.mtsbank.cardoffice.cashback.domain;

/**
 * Repository port: the domain's view of cashback persistence, implemented
 * by an adapter in {@code adapters/out/persistence} (spine Design
 * Paradigm). The domain depends on this interface only, never on JPA/Spring
 * Data directly.
 *
 * {@code cardId} is kept as a parameter only for shape consistency with
 * sibling controllers/use-cases (e.g. {@code RiskBlocksController}/{@code
 * GetRiskBlocksUseCase}) -- it is never used to filter or scope the query
 * (story Boundaries & Constraints: one demo-wide fixture, not one row per
 * card; nothing in the mock or frontend ties cashback to a specific
 * card's identity).
 */
public interface CashbackRepository {

    CashbackSummary getSummary(String cardId);
}
