package ru.mtsbank.cardoffice.riskblocks.adapters.out.external.fssp;

import org.springframework.stereotype.Component;

import ru.mtsbank.cardoffice.riskblocks.domain.ArrestBlock;
import ru.mtsbank.cardoffice.riskblocks.domain.FsspPort;

/**
 * AD-10 stub adapter: stands in for the real external FSSP arrest
 * block-status system, which is Deferred in the spine. Returns the fixed
 * fixture from {@code src/api/mockData.js}'s {@code blocks.arrest} export
 * regardless of {@code cardId} -- unconditional, matching {@code
 * StubOfferEngineAdapter}'s precedent (story 7-risk-blocks Boundaries &
 * Constraints). No simulated upstream failures (story Boundaries &
 * Constraints: {@code UPSTREAM_UNAVAILABLE} stays reserved, unused by this
 * deterministic stub).
 *
 * {@code FIXTURE_AMOUNT_MINOR_UNITS} is the one wire-format correction
 * this story makes (AD-3): the mock's {@code "сумма ареста 15 000 ₽"},
 * embedded only in {@code FIXTURE_TEXT}'s prose, is pulled out into this
 * signed-integer-kopecks value (15 000 ₽ = 1 500 000 kopecks); {@code
 * FIXTURE_TEXT} itself stays byte-for-byte the mock's string.
 */
@Component
public class StubFsspAdapter implements FsspPort {

    private static final String FIXTURE_TITLE = "Арест ФССП";
    private static final String FIXTURE_TEXT = "Пристав: ОСП по г. Москве · сумма ареста 15 000 ₽ · с 02.08.2026";
    private static final long FIXTURE_AMOUNT_MINOR_UNITS = 1_500_000L;

    @Override
    public ArrestBlock getArrestBlock(String cardId) {
        return new ArrestBlock(FIXTURE_TITLE, FIXTURE_TEXT, FIXTURE_AMOUNT_MINOR_UNITS);
    }
}
