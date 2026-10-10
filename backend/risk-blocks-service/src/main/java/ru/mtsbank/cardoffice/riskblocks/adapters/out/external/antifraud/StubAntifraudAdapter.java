package ru.mtsbank.cardoffice.riskblocks.adapters.out.external.antifraud;

import org.springframework.stereotype.Component;

import ru.mtsbank.cardoffice.riskblocks.domain.AntifraudBlock;
import ru.mtsbank.cardoffice.riskblocks.domain.AntifraudPort;

/**
 * AD-10 stub adapter: stands in for the real external antifraud
 * block-status system, which is Deferred in the spine. Returns the fixed
 * fixture from {@code src/api/mockData.js}'s {@code blocks.af} export
 * regardless of {@code cardId} -- unconditional, matching {@code
 * StubOfferEngineAdapter}'s precedent (story 7-risk-blocks Boundaries &
 * Constraints). No simulated upstream failures (story Boundaries &
 * Constraints: {@code UPSTREAM_UNAVAILABLE} stays reserved, unused by this
 * deterministic stub).
 */
@Component
public class StubAntifraudAdapter implements AntifraudPort {

    private static final String FIXTURE_TITLE = "Блокировка АФ";
    private static final String FIXTURE_TEXT = "Карта •••• 7274 · неактивна";

    @Override
    public AntifraudBlock getAntifraudBlock(String cardId) {
        return new AntifraudBlock(FIXTURE_TITLE, FIXTURE_TEXT);
    }
}
