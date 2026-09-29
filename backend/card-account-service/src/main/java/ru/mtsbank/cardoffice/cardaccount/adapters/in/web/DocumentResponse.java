package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import ru.mtsbank.cardoffice.cardaccount.domain.Document;

/**
 * One element of the {@code GET
 * /clients/{clientId}/cards/{cardId}/documents} response array. Mirrors
 * {@code mockData.documents}' fields exactly (AD-8, story Boundaries &
 * Constraints) -- {@code id}, {@code title}, {@code subtitle} -- with no
 * wire-format correction needed (unlike {@code CardResponse}'s money/date
 * fields): a document row is plain strings all the way through.
 */
public record DocumentResponse(String id, String title, String subtitle) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getTitle(), document.getSubtitle());
    }
}
