package ru.mtsbank.cardoffice.cardaccount.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardaccount.domain.DocumentNotFoundException;
import ru.mtsbank.cardoffice.cardaccount.domain.DocumentRepository;

/**
 * CAP-4: the {@code print} stub action. Matches {@code client.js}'s existing
 * {@code printDocument} contract exactly (story Boundaries & Constraints) --
 * no real document generation/print dispatch; the only real behavior added
 * beyond the stateless mock is the not-found check.
 */
@Service
public class PrintDocumentUseCase {

    private final DocumentRepository documentRepository;

    public PrintDocumentUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void execute(String documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
    }
}
