package ru.mtsbank.cardoffice.riskblocks.adapters.in.web;

import ru.mtsbank.cardoffice.riskblocks.domain.AntifraudBlock;
import ru.mtsbank.cardoffice.riskblocks.domain.ArrestBlock;
import ru.mtsbank.cardoffice.riskblocks.domain.RiskBlocks;
import ru.mtsbank.cardoffice.riskblocks.domain.SfmBlock;

/**
 * {@code GET /cards/{cardId}/blocks} response body. Mirrors {@code
 * mockData.blocks}'s shape exactly (AD-8): {@code sfm: {title, text}},
 * {@code arrest: {title, text, amount}}, {@code af: {title, text}}.
 *
 * {@code arrest.amount} is the one wire-format correction this story makes
 * vs. the mock (AD-3 explicitly binds "risk-blocks (arrest sum)") -- see
 * {@code ArrestBlock}'s class Javadoc. {@code sfm}/{@code af} carry no
 * such field; neither block's own text is reshaped.
 */
public record RiskBlocksResponse(SfmResponse sfm, ArrestResponse arrest, AfResponse af) {

    public static RiskBlocksResponse from(RiskBlocks riskBlocks) {
        return new RiskBlocksResponse(
                SfmResponse.from(riskBlocks.getSfm()),
                ArrestResponse.from(riskBlocks.getArrest()),
                AfResponse.from(riskBlocks.getAf()));
    }

    public record SfmResponse(String title, String text) {

        public static SfmResponse from(SfmBlock block) {
            return new SfmResponse(block.getTitle(), block.getText());
        }
    }

    public record ArrestResponse(String title, String text, long amount) {

        public static ArrestResponse from(ArrestBlock block) {
            return new ArrestResponse(block.getTitle(), block.getText(), block.getAmountMinorUnits());
        }
    }

    public record AfResponse(String title, String text) {

        public static AfResponse from(AntifraudBlock block) {
            return new AfResponse(block.getTitle(), block.getText());
        }
    }
}
