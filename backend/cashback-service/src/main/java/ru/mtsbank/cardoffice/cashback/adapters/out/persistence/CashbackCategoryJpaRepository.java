package ru.mtsbank.cardoffice.cashback.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link CashbackCategoryEntity}, used only by
 * {@link JpaCashbackRepository}. Ordered by {@code sort_order} so the
 * response array's row order is deterministic and matches {@code
 * mockData.cashback.categories}' own order exactly (see {@link
 * CashbackCategoryEntity} Javadoc: that order isn't alphabetical by {@code
 * id}, so relying on {@code id} ordering would not reproduce it).
 */
public interface CashbackCategoryJpaRepository extends JpaRepository<CashbackCategoryEntity, String> {

    List<CashbackCategoryEntity> findAllByOrderBySortOrderAsc();
}
