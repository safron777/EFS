package ru.mtsbank.cardoffice.riskblocks.domain;

/**
 * AD-10 stub-port pattern: the domain's boundary to the external FSSP
 * (bailiff arrest) block-status system -- one port per external system
 * (AD-10), never a shared/catch-all port across SFM, FSSP and antifraud.
 * The demo binds this to a stub adapter in {@code
 * adapters/out/external/fssp/} (see {@code StubFsspAdapter}); a later real
 * integration adds a new adapter implementation only -- this interface,
 * and the domain and application code depending on it, must not change.
 */
public interface FsspPort {

    ArrestBlock getArrestBlock(String cardId);
}
