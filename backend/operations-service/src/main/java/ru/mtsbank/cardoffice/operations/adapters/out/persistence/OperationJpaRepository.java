package ru.mtsbank.cardoffice.operations.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link OperationEntity}, used only by
 * {@link JpaOperationsRepository}. Ordered by {@code op_date} descending so
 * the response array's row order is deterministic and newest-first,
 * matching {@code mockData.operations.rows}' own order (story Boundaries &
 * Constraints); {@code id} ascending is a secondary tiebreaker so two rows
 * sharing the same {@code op_date} still have a fully deterministic order,
 * not an unspecified one.
 */
public interface OperationJpaRepository extends JpaRepository<OperationEntity, String> {

    List<OperationEntity> findByCardIdOrderByOpDateDescIdAsc(String cardId);
}
