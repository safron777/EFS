package ru.mtsbank.cardoffice.clientidentity.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository over {@link ClientEntity}, used only by {@link JpaClientRepository}. */
public interface ClientJpaRepository extends JpaRepository<ClientEntity, String> {
}
