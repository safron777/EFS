package ru.mtsbank.cardoffice.cardaccount.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardaccount.domain.DocumentNotFoundException;
import ru.mtsbank.cardoffice.cardaccount.domain.DocumentRepository;

/**
 * CAP-4: the {@code send} stub action. Matches {@code client.js}'s existing
 * {@code sendDocument} contract exactly (story Boundaries & Constraints) --
 * no real email dispatch, no {@code channel} validation/branching (the
 * frontend always calls with the default {@code "email"} and no UI ever
 * varies it); the only real behavior added beyond the stateless mock is the
 * not-found check.
 */
@Service
public class SendDocumentUseCase {

    private final DocumentRepository documentRepository;

    public SendDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void execute(String documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
    }
}
