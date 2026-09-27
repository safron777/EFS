package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over {@link CardEntity}, used only by
 * {@link JpaCardRepository}. Ordered {@code isMain} first (descending, so
 * {@code true} sorts before {@code false}) then by {@code id} so the
 * response array's row order is deterministic and matches
 * {@code mockData.cards}' own order (main card first).
 */
public interface CardJpaRepository extends JpaRepository<CardEntity, String> {

    List<CardEntity> findByClientIdAndProductIdOrderByIsMainDescIdAsc(String clientId, String productId);
}
