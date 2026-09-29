package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link DocumentEntity}, used only by
 * {@link JpaDocumentRepository}. Ordered by {@code sort_order} so the
 * response array's row order is deterministic and matches
 * {@code mockData.documents}' own order exactly (see {@link DocumentEntity}
 * Javadoc: that order isn't alphabetical by {@code id}, so relying on
 * {@code id} ordering would not reproduce it).
 */
public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findAllByOrderBySortOrderAsc();
}
