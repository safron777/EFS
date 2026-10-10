package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

import java.util.List;

import ru.mtsbank.cardoffice.cardissuance.domain.CurrentApplication;
import ru.mtsbank.cardoffice.cardissuance.domain.Issuance;
import ru.mtsbank.cardoffice.cardissuance.domain.NewCardInfo;

/**
 * {@code GET /clients/{clientId}/products/{productId}/issuance} response
 * body. Mirrors {@code mockData.issuance}'s three top-level fields exactly
 * (AD-8, story Boundaries & Constraints): {@code currentApplication},
 * {@code newCard}, {@code history} (with {@code history[].date} corrected to
 * ISO-8601, see {@link ApplicationResponse}).
 */
public record IssuanceResponse(
        CurrentApplicationResponse currentApplication, NewCardResponse newCard, List<ApplicationResponse> history) {

    public static IssuanceResponse from(Issuance issuance) {
        return new IssuanceResponse(
                CurrentApplicationResponse.from(issuance.getCurrentApplication()),
                NewCardResponse.from(issuance.getNewCard()),
                issuance.getHistory().stream().map(ApplicationResponse::from).toList());
    }

    public record CurrentApplicationResponse(String card, String status, String text) {

        public static CurrentApplicationResponse from(CurrentApplication currentApplication) {
            return new CurrentApplicationResponse(
                    currentApplication.getCard(), currentApplication.getStatus(), currentApplication.getText());
        }
    }

    public record NewCardResponse(String text) {

        public static NewCardResponse from(NewCardInfo newCard) {
            return new NewCardResponse(newCard.getText());
        }
    }
}
