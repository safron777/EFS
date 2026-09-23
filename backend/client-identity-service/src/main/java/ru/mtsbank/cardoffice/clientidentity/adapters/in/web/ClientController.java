package ru.mtsbank.cardoffice.clientidentity.adapters.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.mtsbank.cardoffice.clientidentity.application.GetClientCardUseCase;
import ru.mtsbank.cardoffice.clientidentity.domain.Client;

/**
 * CAP-1 REST resource: {@code GET /clients/{id}} (AD-8). Read-only -- no
 * mutation endpoints belong here (story Boundaries & Constraints). Translates
 * HTTP to the application layer only; no business logic lives here (spine
 * Design Paradigm).
 */
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final GetClientCardUseCase getClientCardUseCase;

    public ClientController(GetClientCardUseCase getClientCardUseCase) {
        this.getClientCardUseCase = getClientCardUseCase;
    }

    @GetMapping("/{id}")
    public ClientCardResponse getClientCard(@PathVariable("id") String id) {
        Client client = getClientCardUseCase.execute(id);
        return ClientCardResponse.from(client);
    }
}
