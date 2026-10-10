package ru.mtsbank.cardoffice.cardissuance.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository over {@link IssuanceApplicationEntity}, used only by
 * {@link JpaIssuanceRepository}. Ordered by {@code sort_order} so the
 * response array's row order is deterministic and matches
 * {@code mockData.issuance.history}'s own order exactly (see
 * {@link IssuanceApplicationEntity} Javadoc: that order isn't chronological,
 * so relying on {@code application_date} ordering would not reproduce it).
 */
public interface IssuanceApplicationJpaRepository extends JpaRepository<IssuanceApplicationEntity, String> {

    List<IssuanceApplicationEntity> findAllByOrderBySortOrderAsc();

    @Query("select coalesce(max(e.sortOrder), -1) from IssuanceApplicationEntity e")
    int findMaxSortOrder();
}
