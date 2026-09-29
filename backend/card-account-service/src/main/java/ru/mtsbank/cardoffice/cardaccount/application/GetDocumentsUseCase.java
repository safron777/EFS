package ru.mtsbank.cardoffice.cardaccount.application;

import java.util.List;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.cardaccount.domain.Document;
import ru.mtsbank.cardoffice.cardaccount.domain.DocumentRepository;

/**
 * CAP-4: fetch the account document list. Pure pass-through over the
 * {@link DocumentRepository} port -- documents are global (story Design
 * Notes), so {@code clientId}/{@code cardId} are accepted by the controller
 * for URL-shape fidelity only and never reach this use case or the query
 * (story Boundaries & Constraints: no cross-check that a document belongs
 * to the given client/card).
 */
@Service
public class GetDocumentsUseCase {

    private final DocumentRepository documentRepository;

    public GetDocumentsUseCase(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<Document> execute() {
        return documentRepository.findAll();
    }
}
