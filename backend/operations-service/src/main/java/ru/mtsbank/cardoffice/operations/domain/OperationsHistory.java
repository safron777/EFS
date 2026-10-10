package ru.mtsbank.cardoffice.operations.domain;

import java.util.List;

/**
 * CAP-6 aggregate returned by {@code GET /cards/{cardId}/operations}: a
 * nullable {@link FailedTransferAlert} plus the ordered (newest-first)
 * {@link Operation} rows for one card. Framework-free by design (spine
 * Design Paradigm).
 *
 * {@code alert} is legitimately {@code null} -- most cards (any {@code
 * cardId} other than {@code card-4019}) have no failed-transfer alert at
 * all, not merely an empty one (story Boundaries & Constraints).
 */
public final class OperationsHistory {

    private final FailedTransferAlert alert;
    private final List<Operation> rows;

    public OperationsHistory(FailedTransferAlert alert, List<Operation> rows) {
        this.alert = alert;
        this.rows = List.copyOf(rows);
    }

    public FailedTransferAlert getAlert() {
        return alert;
    }

    public List<Operation> getRows() {
        return rows;
    }
}
