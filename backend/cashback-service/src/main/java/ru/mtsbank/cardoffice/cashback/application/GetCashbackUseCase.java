package ru.mtsbank.cardoffice.cashback.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cashback.domain.CashbackRepository;
import ru.mtsbank.cardoffice.cashback.domain.CashbackSummary;

/**
 * CAP-8: fetch the cashback summary for a card (story 8-cashback Intent).
 * Pure pass-through over the {@link CashbackRepository} port -- {@code
 * cardId} is accepted for shape consistency only and never used to filter
 * (story Boundaries & Constraints: one demo-wide fixture, not one row per
 * card).
 */
@Service
public class GetCashbackUseCase {

    private final CashbackRepository cashbackRepository;

    public GetCashbackUseCase(CashbackRepository cashbackRepository) {
        this.cashbackRepository = cashbackRepository;
    }

    public CashbackSummary execute(String cardId) {
        return cashbackRepository.getSummary(cardId);
    }
}
