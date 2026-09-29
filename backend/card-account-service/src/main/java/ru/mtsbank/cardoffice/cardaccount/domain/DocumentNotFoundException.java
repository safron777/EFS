package ru.mtsbank.cardoffice.cardaccount.domain;

/**
 * Thrown when {@code send}/{@code print} is called with a {@code documentId}
 * that doesn't exist. Mapped to {@code 404} with the spine's error envelope
 * ({@code NOT_FOUND}) by the web adapter's exception handler -- this class
 * itself carries no HTTP concerns. Mirrors {@code client-identity-service}'s
 * {@code ClientNotFoundException} shape (story Boundaries & Constraints:
 * reuse the existing corrected {@code CardExceptionHandler}, no second
 * advice class).
 */
public class DocumentNotFoundException extends RuntimeException {

    private final String documentId;

    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
        this.documentId = documentId;
    }

    public String getDocumentId() {
        return documentId;
    }
}
