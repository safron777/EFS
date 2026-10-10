package ru.mtsbank.cardoffice.operations.adapters.in.web;

import java.time.LocalDate;
import java.util.List;

import ru.mtsbank.cardoffice.operations.domain.FailedTransferAlert;
import ru.mtsbank.cardoffice.operations.domain.Operation;
import ru.mtsbank.cardoffice.operations.domain.OperationsHistory;

/**
 * {@code GET /cards/{cardId}/operations} response body. Mirrors {@code
 * mockData.operations}' shape exactly (AD-8): {@code failedAlert} (an
 * object, or {@code null} when the card has none) plus {@code rows}, each
 * {@code {id, date, op, sum, status}}.
 *
 * {@code failedAlert} is intentionally not annotated {@code
 * @JsonInclude(NON_NULL)}: a {@code null} alert must serialize as {@code
 * "failedAlert":null}, matching the response shape fixed by the story
 * (same rationale as CAP-1's {@code ClientCardResponse.TriggerResponse
 * .link}).
 *
 * Two wire-format corrections vs. the mock, per story Boundaries &
 * Constraints: {@code RowResponse.sum} is a signed integer in kopecks
 * (AD-3), not the mock's formatted {@code "−5 000 ₽"} string; {@code
 * RowResponse.date} is ISO-8601 ({@link LocalDate}), not the mock's
 * year-less {@code "12.09"}. {@code RowResponse.status} is deliberately
 * left as the mock's exact literal Russian string -- see {@code
 * Operation}'s class Javadoc.
 */
public record OperationsResponse(FailedAlertResponse failedAlert, List<RowResponse> rows) {

    public static OperationsResponse from(OperationsHistory history) {
        FailedAlertResponse alert =
                history.getAlert() == null ? null : FailedAlertResponse.from(history.getAlert());
        List<RowResponse> rows = history.getRows().stream().map(RowResponse::from).toList();
        return new OperationsResponse(alert, rows);
    }

    public record FailedAlertResponse(String title, String text) {

        public static FailedAlertResponse from(FailedTransferAlert alert) {
            return new FailedAlertResponse(alert.getTitle(), alert.getText());
        }
    }

    public record RowResponse(String id, LocalDate date, String op, long sum, String status) {

        public static RowResponse from(Operation operation) {
            return new RowResponse(
                    operation.getId(),
                    operation.getDate(),
                    operation.getDescription(),
                    operation.getAmountMinorUnits(),
                    operation.getStatus());
        }
    }
}
