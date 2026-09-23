package ru.mtsbank.cardoffice.clientidentity.domain;

/**
 * Thrown when no client exists for a given id. Mapped to {@code 404} with the
 * spine's error envelope ({@code NOT_FOUND}) by the web adapter's exception
 * handler -- this class itself carries no HTTP concerns.
 */
public class ClientNotFoundException extends RuntimeException {

    private final String clientId;

    public ClientNotFoundException(String clientId) {
        super("Client not found: " + clientId);
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
