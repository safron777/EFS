package ru.mtsbank.cardoffice.operations.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link OperationAlertEntity}, used only by
 * {@link JpaOperationsRepository}. {@code card_id} is the primary key, so
 * the inherited {@code findById} is exactly the 0-or-1-per-card lookup CAP-6
 * needs -- no custom query method required.
 */
public interface OperationAlertJpaRepository extends JpaRepository<OperationAlertEntity, String> {
}
