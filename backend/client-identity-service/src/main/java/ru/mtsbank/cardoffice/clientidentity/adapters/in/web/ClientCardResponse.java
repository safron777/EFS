package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

import java.util.List;

import ru.mtsbank.cardoffice.clientidentity.domain.Client;
import ru.mtsbank.cardoffice.clientidentity.domain.ClientTrigger;

/**
 * {@code GET /clients/{id}} response body. Mirrors {@code mockData.client}'s
 * shape (AD-8) except {@code openCaseIds: string[]} in place of the mock's
 * {@code openCases: [{id, label}]} -- the deliberate CAP-1 divergence
 * documented in the story's Design Notes (AD-2: no denormalized copy of
 * incident-case-service's label text).
 *
 * {@code TriggerResponse.link} is intentionally not annotated
 * {@code @JsonInclude(NON_NULL)}: the I/O matrix requires a {@code null} link
 * to serialize as {@code "link":null}, not be omitted.
 */
public record ClientCardResponse(
        String id,
        String fullName,
        int clientSince,
        List<String> badges,
        List<TriggerResponse> triggers,
        List<String> openCaseIds) {

    public static ClientCardResponse from(Client client) {
        List<TriggerResponse> triggers = client.getTriggers().stream()
                .map(TriggerResponse::from)
                .toList();
        return new ClientCardResponse(
                client.getId(),
                client.getFullName(),
                client.getClientSince(),
                client.getBadges(),
                triggers,
                client.getOpenCaseIds());
    }

    public record TriggerResponse(String id, String icon, String tone, String title, String text, String link) {

        public static TriggerResponse from(ClientTrigger trigger) {
            return new TriggerResponse(
                    trigger.getId(),
                    trigger.getIcon(),
                    trigger.getTone(),
                    trigger.getTitle(),
                    trigger.getText(),
                    trigger.getLink());
        }
    }
}
