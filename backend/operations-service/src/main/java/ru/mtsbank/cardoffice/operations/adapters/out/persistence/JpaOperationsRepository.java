package ru.mtsbank.cardoffice.operations.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.operations.domain.FailedTransferAlert;
import ru.mtsbank.cardoffice.operations.domain.Operation;
import ru.mtsbank.cardoffice.operations.domain.OperationsHistory;
import ru.mtsbank.cardoffice.operations.domain.OperationsRepository;

/**
 * Implements the domain {@link OperationsRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link OperationEntity}/{@link
 * OperationAlertEntity} (JPA) and {@link Operation}/{@link
 * FailedTransferAlert} (domain).
 *
 * An unmatched {@code cardId} naturally yields an empty row list and no
 * alert row from both queries -- no explicit not-found branch needed (story
 * Boundaries & Constraints: {@code 200} with {@code {failedAlert: null,
 * rows: []}}).
 */
@Repository
public class JpaOperationsRepository implements OperationsRepository {

    private final OperationJpaRepository operationJpaRepository;
    private final OperationAlertJpaRepository operationAlertJpaRepository;

    public JpaOperationsRepository(
            OperationJpaRepository operationJpaRepository,
            OperationAlertJpaRepository operationAlertJpaRepository) {
        this.operationJpaRepository = operationJpaRepository;
        this.operationAlertJpaRepository = operationAlertJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OperationsHistory getHistory(String cardId) {
        List<Operation> rows = operationJpaRepository.findByCardIdOrderByOpDateDescIdAsc(cardId).stream()
                .map(JpaOperationsRepository::toDomain)
                .toList();
        FailedTransferAlert alert = operationAlertJpaRepository
                .findById(cardId)
                .map(JpaOperationsRepository::toDomain)
                .orElse(null);
        return new OperationsHistory(alert, rows);
    }

    private static Operation toDomain(OperationEntity entity) {
        return new Operation(
                entity.getId(),
                entity.getOpDate(),
                entity.getDescription(),
                entity.getAmountMinorUnits(),
                entity.getStatus());
    }

    private static FailedTransferAlert toDomain(OperationAlertEntity entity) {
        return new FailedTransferAlert(entity.getTitle(), entity.getText());
    }
}
