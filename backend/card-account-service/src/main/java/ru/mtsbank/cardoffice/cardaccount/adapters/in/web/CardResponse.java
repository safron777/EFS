package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import ru.mtsbank.cardoffice.cardaccount.domain.Card;

/**
 * One element of the {@code GET /clients/{clientId}/products/{productId}/cards}
 * response array. Mirrors {@code mockData.cards}' fields (AD-8) --
 * {@code id}, {@code product}, {@code masked}, {@code subtitle},
 * {@code isMain}, {@code status}, {@code balance}, {@code issuedAt},
 * {@code account}, {@code closedAt} -- with two deliberate wire-format
 * corrections the mock itself doesn't make (story Boundaries &
 * Constraints):
 *
 * <ul>
 *   <li>{@code balance} is a signed integer in kopecks (AD-3), not the
 *       mock's raw-ruble number.</li>
 *   <li>{@code issuedAt}/{@code closedAt} are ISO-8601 dates ({@link
 *       LocalDate}, serialized as {@code "2025-01-12"}), not the mock's
 *       {@code "dd.MM.yyyy"} strings.</li>
 * </ul>
 *
 * {@code @JsonInclude(NON_NULL)} at the class level is intentional here --
 * the opposite choice from CAP-1's {@code ClientCardResponse.TriggerResponse
 * .link}. There, a {@code null} link had to serialize as {@code "link":null}
 * because the mock itself always includes the key. Here, the mock's own
 * card objects simply omit {@code account}/{@code issuedAt}/{@code closedAt}
 * entirely on the rows that don't have them (an active card has no
 * {@code closedAt} key at all; a closed card has no {@code account}/
 * {@code issuedAt} keys) -- so reproducing "per-row field presence exactly"
 * means omitting the key, not emitting an explicit {@code null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardResponse(
        String id,
        String product,
        String masked,
        String subtitle,
        boolean isMain,
        String status,
        long balance,
        LocalDate issuedAt,
        String account,
        LocalDate closedAt) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getProduct(),
                card.getMasked(),
                card.getSubtitle(),
                card.isMain(),
                card.getStatus(),
                card.getBalanceMinorUnits(),
                card.getIssuedAt(),
                card.getAccount(),
                card.getClosedAt());
    }
}
