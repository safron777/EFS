package ru.mtsbank.cardoffice.cardissuance.adapters.in.web;

import java.time.LocalDate;

import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceApplication;

/**
 * One issuance-history row: either an element of {@code GET
 * .../issuance}'s {@code history} array, or the created row returned by
 * {@code POST .../issuance/applications} (both {@code 200} and {@code 201}
 * bodies share this exact shape). Mirrors {@code mockData.issuance.history}'s
 * fields (AD-8) -- {@code id}, {@code card}, {@code method}, {@code status}
 * -- with {@code date} as ISO-8601 ({@link LocalDate}), the one wire-format
 * correction story Boundaries & Constraints makes to the mock's
 * {@code "01.09"}/{@code "12.01.2025"}/{@code "20.10.2025"} strings.
 */
public record ApplicationResponse(String id, LocalDate date, String card, String method, String status) {

    public static ApplicationResponse from(IssuanceApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getDate(),
                application.getCard(),
                application.getMethod(),
                application.getStatus());
    }
}
