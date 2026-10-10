package ru.mtsbank.cardoffice.riskblocks.adapters.out.external.sfm;

import org.springframework.stereotype.Component;

import ru.mtsbank.cardoffice.riskblocks.domain.SfmBlock;
import ru.mtsbank.cardoffice.riskblocks.domain.SfmPort;

/**
 * AD-10 stub adapter: stands in for the real external SFM block-status
 * system, which is Deferred in the spine. Returns the fixed fixture from
 * {@code src/api/mockData.js}'s {@code blocks.sfm} export regardless of
 * {@code cardId} -- unconditional, matching {@code
 * StubOfferEngineAdapter}'s precedent (story 7-risk-blocks Boundaries &
 * Constraints). No simulated upstream failures (story Boundaries &
 * Constraints: {@code UPSTREAM_UNAVAILABLE} stays reserved, unused by this
 * deterministic stub).
 */
@Component
public class StubSfmAdapter implements SfmPort {

    private static final String FIXTURE_TITLE = "Блокировка СФМ / Списки";
    private static final String FIXTURE_TEXT =
            "Карта •••• 4019 · с 15.09.2026 · причина: проверка операции на 5 000 ₽";

    @Override
    public SfmBlock getSfmBlock(String cardId) {
        return new SfmBlock(FIXTURE_TITLE, FIXTURE_TEXT);
    }
}
