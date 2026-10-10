package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link CashbackSummaryEntity}, used only by
 * {@link JpaCashbackRepository}. The table is a singleton (one row, fixed
 * id {@value JpaCashbackRepository#SUMMARY_ID}), so no custom finder is
 * needed beyond the inherited {@code findById}.
 */
public interface CashbackSummaryJpaRepository extends JpaRepository<CashbackSummaryEntity, String> {
}
