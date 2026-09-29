package ru.mtsbank.cardoffice.cardaccount.adapters.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.cardaccount.application.GetDocumentsUseCase;
import ru.mtsbank.cardoffice.cardaccount.application.PrintDocumentUseCase;
import ru.mtsbank.cardoffice.cardaccount.application.SendDocumentUseCase;

/**
 * CAP-4 REST resource, mirroring {@code client.js}'s three document calls
 * exactly (AD-8): {@code GET /clients/{clientId}/cards/{cardId}/documents},
 * {@code POST /clients/{clientId}/documents/{documentId}/send}, {@code POST
 * /clients/{clientId}/documents/{documentId}/print}. Translates HTTP to the
 * application layer only; no business logic lives here (spine Design
 * Paradigm).
 *
 * {@code clientId}/{@code cardId}/{@code documentId} path variables are
 * accepted at face value and never cross-checked against each other --
 * documents are global in this demo (story Boundaries & Constraints, AD-9).
 * {@code channel} on the send request body is optional and deliberately
 * unread/unvalidated (story Boundaries & Constraints: the frontend always
 * sends the default {@code "email"} and no UI ever varies it).
 */
@RestController
public class DocumentController {

    private final GetDocumentsUseCase getDocumentsUseCase;
    private final SendDocumentUseCase sendDocumentUseCase;
    private final PrintDocumentUseCase printDocumentUseCase;

    public DocumentController(
            GetDocumentsUseCase getDocumentsUseCase,
            SendDocumentUseCase sendDocumentUseCase,
            PrintDocumentUseCase printDocumentUseCase) {
        this.getDocumentsUseCase = getDocumentsUseCase;
        this.sendDocumentUseCase = sendDocumentUseCase;
        this.printDocumentUseCase = printDocumentUseCase;
    }

    @GetMapping("/clients/{clientId}/cards/{cardId}/documents")
    public List<DocumentResponse> getDocuments(
            // clientId/cardId are bound only to match the URL shape (AD-8) -- intentionally
            // unused in the body: documents are global in this demo, no per-client/card query
            // or cross-check (story Boundaries & Constraints; see class Javadoc).
            @PathVariable("clientId") String clientId, @PathVariable("cardId") String cardId) {
        return getDocumentsUseCase.execute().stream().map(DocumentResponse::from).toList();
    }

    @PostMapping("/clients/{clientId}/documents/{documentId}/send")
    public OkResponse sendDocument(
            // clientId is bound only to match the URL shape -- intentionally unused, same
            // rationale as getDocuments above.
            @PathVariable("clientId") String clientId,
            @PathVariable("documentId") String documentId,
            @RequestBody(required = false) SendDocumentRequest request) {
        sendDocumentUseCase.execute(documentId);
        return OkResponse.OK;
    }

    @PostMapping("/clients/{clientId}/documents/{documentId}/print")
    public OkResponse printDocument(
            // clientId is bound only to match the URL shape -- intentionally unused, same
            // rationale as getDocuments above.
            @PathVariable("clientId") String clientId, @PathVariable("documentId") String documentId) {
        printDocumentUseCase.execute(documentId);
        return OkResponse.OK;
    }

    /**
     * Request body for {@code send}. {@code channel} is optional and
     * intentionally unused beyond deserialization -- see class Javadoc.
     */
    public record SendDocumentRequest(String channel) {
    }
}
