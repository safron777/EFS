package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.cashback.domain.CashbackCategory;
import ru.mtsbank.cardoffice.cashback.domain.CashbackPromo;
import ru.mtsbank.cardoffice.cashback.domain.CashbackRepository;
import ru.mtsbank.cardoffice.cashback.domain.CashbackSummary;

/**
 * Implements the domain {@link CashbackRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link CashbackSummaryEntity}/{@link
 * CashbackCategoryEntity} (JPA) and {@link CashbackSummary}/{@link
 * CashbackCategory}/{@link CashbackPromo} (domain).
 *
 * {@code cardId} is accepted by the port's method signature for shape
 * consistency with sibling repositories only -- it is never used here to
 * filter either table (story Boundaries & Constraints: one demo-wide
 * fixture).
 */
@Repository
public class JpaCashbackRepository implements CashbackRepository {

    /** The one {@code cashback_summary} row's fixed id, seeded by 002-seed-cashback.xml. */
    static final String SUMMARY_ID = "cashback-summary";

    private final CashbackSummaryJpaRepository summaryJpaRepository;
    private final CashbackCategoryJpaRepository categoryJpaRepository;

    public JpaCashbackRepository(
            CashbackSummaryJpaRepository summaryJpaRepository, CashbackCategoryJpaRepository categoryJpaRepository) {
        this.summaryJpaRepository = summaryJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CashbackSummary getSummary(String cardId) {
        CashbackSummaryEntity summaryEntity = summaryJpaRepository
                .findById(SUMMARY_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "cashback_summary singleton row (" + SUMMARY_ID + ") is missing -- seed data not applied"));

        List<CashbackCategory> categories = categoryJpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(JpaCashbackRepository::toDomainCategory)
                .toList();

        CashbackPromo promo = new CashbackPromo(summaryEntity.getPromoTitle(), summaryEntity.getPromoText());

        return new CashbackSummary(summaryEntity.getAccumulatedMinorUnits(), categories, promo);
    }

    private static CashbackCategory toDomainCategory(CashbackCategoryEntity entity) {
        return new CashbackCategory(entity.getId(), entity.getLabel(), entity.getPercent(), entity.getTone());
    }
}
