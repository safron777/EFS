package ru.mtsbank.cardoffice.incidentcase.adapters.in.web;

/**
 * {@code {"ok": true}} -- the attach mutation's response body, matching
 * {@code client.js}'s existing {@code attachClientToIncident} stub contract
 * exactly (story Boundaries & Constraints) and the spine Consistency
 * Conventions' baseline mutation response ({@code { ok: true }}). Mirrors
 * {@code notifications-service}'s {@code OkResponse} shape exactly.
 */
public record OkResponse(boolean ok) {

    public static final OkResponse OK = new OkResponse(true);
}
