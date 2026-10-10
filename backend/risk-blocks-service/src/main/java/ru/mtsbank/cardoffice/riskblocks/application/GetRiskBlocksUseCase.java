package ru.mtsbank.cardoffice.riskblocks.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.riskblocks.domain.AntifraudPort;
import ru.mtsbank.cardoffice.riskblocks.domain.FsspPort;
import ru.mtsbank.cardoffice.riskblocks.domain.RiskBlocks;
import ru.mtsbank.cardoffice.riskblocks.domain.SfmPort;

/**
 * CAP-7: fetch a card's risk-block status by calling all three independent
 * external-system ports and composing the result (story 7-risk-blocks
 * Intent). {@code cardId} is accepted at face value and passed straight
 * through to each port -- it is never verified against {@code
 * card-account-service} (AD-9: no synchronous inter-service calls; story
 * Boundaries & Constraints), and there is no not-found case for this
 * capability (all three blocks are unconditionally present for any {@code
 * cardId}), so this use case orchestrates the three ports with no
 * branching logic beyond the calls.
 */
@Service
public class GetRiskBlocksUseCase {

    private final SfmPort sfmPort;
    private final FsspPort fsspPort;
    private final AntifraudPort antifraudPort;

    public GetRiskBlocksUseCase(SfmPort sfmPort, FsspPort fsspPort, AntifraudPort antifraudPort) {
        this.sfmPort = sfmPort;
        this.fsspPort = fsspPort;
        this.antifraudPort = antifraudPort;
    }

    public RiskBlocks execute(String cardId) {
        return new RiskBlocks(
                sfmPort.getSfmBlock(cardId), fsspPort.getArrestBlock(cardId), antifraudPort.getAntifraudBlock(cardId));
    }
}
