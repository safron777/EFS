package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

/**
 * {@code {"ok": true}} -- the response body for the {@code send}/{@code
 * print} stub actions, matching {@code client.js}'s existing stub contract
 * exactly (story Boundaries & Constraints) and the spine Consistency
 * Conventions' baseline mutation response ({@code { ok: true }}).
 */
public record OkResponse(boolean ok) {

    public static final OkResponse OK = new OkResponse(true);
}
