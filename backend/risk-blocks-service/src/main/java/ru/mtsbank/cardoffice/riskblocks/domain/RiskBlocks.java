package ru.mtsbank.cardoffice.riskblocks.domain;

import java.util.Objects;

/**
 * CAP-7 aggregate returned by {@code GET /cards/{cardId}/blocks}: the three
 * independently-sourced blocks composed into one response (story
 * 7-risk-blocks Intent). Framework-free by design (spine Design Paradigm).
 *
 * All three fields are always non-null for this stub-backed demo -- every
 * card gets the same always-active fixture set (story Boundaries &
 * Constraints; {@code BlocksPage.jsx} dereferences all three with no null
 * guards, so a null block here would crash the existing frontend).
 */
public final class RiskBlocks {

    private final SfmBlock sfm;
    private final ArrestBlock arrest;
    private final AntifraudBlock af;

    public RiskBlocks(SfmBlock sfm, ArrestBlock arrest, AntifraudBlock af) {
        this.sfm = Objects.requireNonNull(sfm, "sfm");
        this.arrest = Objects.requireNonNull(arrest, "arrest");
        this.af = Objects.requireNonNull(af, "af");
    }

    public SfmBlock getSfm() {
        return sfm;
    }

    public ArrestBlock getArrest() {
        return arrest;
    }

    public AntifraudBlock getAf() {
        return af;
    }
}
