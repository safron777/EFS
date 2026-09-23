package ru.mtsbank.cardoffice.clientidentity.adapters.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.clientidentity.domain.Client;
import ru.mtsbank.cardoffice.clientidentity.domain.ClientRepository;
import ru.mtsbank.cardoffice.clientidentity.domain.ClientTrigger;

/**
 * Implements the domain {@link ClientRepository} port against this service's
 * own PostgreSQL schema (AD-1: DB-per-service). The only place that
 * translates between {@link ClientEntity}/{@link ClientTriggerEntity} (JPA)
 * and {@link Client}/{@link ClientTrigger} (domain).
 */
@Repository
public class JpaClientRepository implements ClientRepository {

    private final ClientJpaRepository jpaRepository;

    public JpaClientRepository(ClientJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(String id) {
        return jpaRepository.findById(id).map(JpaClientRepository::toDomain);
    }

    private static Client toDomain(ClientEntity entity) {
        List<ClientTrigger> triggers = entity.getTriggers().stream()
                .map(t -> new ClientTrigger(t.getTriggerKey(), t.getIcon(), t.getTone(), t.getTitle(), t.getText(), t.getLink()))
                .toList();
        return new Client(
                entity.getId(),
                entity.getFullName(),
                entity.getClientSince(),
                entity.getBadges(),
                triggers,
                entity.getOpenCaseIds());
    }
}
