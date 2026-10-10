package ru.mtsbank.cardoffice.cashback.adapters.in.web;

import java.util.List;

import ru.mtsbank.cardoffice.cashback.domain.CashbackCategory;
import ru.mtsbank.cardoffice.cashback.domain.CashbackPromo;
import ru.mtsbank.cardoffice.cashback.domain.CashbackSummary;

/**
 * {@code GET /cards/{cardId}/cashback} response body. Mirrors {@code
 * mockData.cashback}'s shape exactly (AD-8): {@code accumulated}, {@code
 * categories: [{id, label, percent, tone}]}, {@code promo: {title, text}}.
 *
 * {@code accumulated} is the one wire-format correction this story makes
 * vs. the mock (AD-3 explicitly binds "cashback") -- the mock's raw-ruble
 * {@code 1240} becomes the kopecks integer {@code 124000}. {@code
 * percent}/{@code tone} carry no such correction -- they stay the mock's
 * exact literal strings (story Boundaries & Constraints).
 */
public record CashbackResponse(long accumulated, List<CategoryResponse> categories, PromoResponse promo) {

    public static CashbackResponse from(CashbackSummary summary) {
        return new CashbackResponse(
                summary.getAccumulatedMinorUnits(),
                summary.getCategories().stream().map(CategoryResponse::from).toList(),
                PromoResponse.from(summary.getPromo()));
    }

    public record CategoryResponse(String id, String label, String percent, String tone) {

        public static CategoryResponse from(CashbackCategory category) {
            return new CategoryResponse(
                    category.getId(), category.getLabel(), category.getPercent(), category.getTone());
        }
    }

    public record PromoResponse(String title, String text) {

        public static PromoResponse from(CashbackPromo promo) {
            return new PromoResponse(promo.getTitle(), promo.getText());
        }
    }
}
