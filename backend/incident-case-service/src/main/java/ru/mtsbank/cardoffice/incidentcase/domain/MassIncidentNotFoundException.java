package ru.mtsbank.cardoffice.incidentcase.domain;

/**
 * Thrown when {@code attach} is called with an {@code incidentId} that
 * doesn't exist. Mapped to {@code 404} with the spine's error envelope
 * ({@code NOT_FOUND}) by the web adapter's exception handler -- this class
 * itself carries no HTTP concerns. Mirrors {@code notifications-service}'s
 * {@code NotificationChannelNotFoundException} shape exactly (story
 * Boundaries & Constraints).
 */
public class MassIncidentNotFoundException extends RuntimeException {

    private final String incidentId;

    public MassIncidentNotFoundException(String incidentId) {
        super("Mass incident not found: " + incidentId);
        this.incidentId = incidentId;
    }

    public String getIncidentId() {
        return incidentId;
    }
}
