package ru.mtsbank.cardoffice.clientidentity.application;

import org.springframework.stereotype.Service;

import ru.mtsbank.cardoffice.clientidentity.domain.Client;
import ru.mtsbank.cardoffice.clientidentity.domain.ClientNotFoundException;
import ru.mtsbank.cardoffice.clientidentity.domain.ClientRepository;

/**
 * CAP-1: fetch a client's identity card (profile, badges, triggers,
 * open-case ids) by id. Orchestrates the {@link ClientRepository} port and
 * turns a miss into a domain-level {@link ClientNotFoundException} -- the web
 * adapter maps that to the HTTP 404 + error envelope.
 */
@Service
public class GetClientCardUseCase {

    private final ClientRepository clientRepository;

    public GetClientCardUseCase(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client execute(String clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }
}
