package ru.mtsbank.cardoffice.notifications.adapters.in.web;

/**
 * {@code {"ok": true}} -- the toggle mutation's response body, matching
 * {@code client.js}'s existing {@code toggleNotification} stub contract
 * exactly (story Boundaries & Constraints) and the spine Consistency
 * Conventions' baseline mutation response ({@code { ok: true }}). Mirrors
 * {@code card-account-service}'s {@code OkResponse} shape exactly.
 */
public record OkResponse(boolean ok) {

    public static final OkResponse OK = new OkResponse(true);
}
