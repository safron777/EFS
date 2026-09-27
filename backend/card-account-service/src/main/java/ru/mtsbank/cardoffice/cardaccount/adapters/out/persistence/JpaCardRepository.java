package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.cardaccount.domain.Card;
import ru.mtsbank.cardoffice.cardaccount.domain.CardRepository;

/**
 * Implements the domain {@link CardRepository} port against this service's
 * own PostgreSQL schema (AD-1: DB-per-service). The only place that
 * translates between {@link CardEntity} (JPA) and {@link Card} (domain).
 */
@Repository
public class JpaCardRepository implements CardRepository {

    private final CardJpaRepository jpaRepository;

    public JpaCardRepository(CardJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> findByClientIdAndProductId(String clientId, String productId) {
        return jpaRepository.findByClientIdAndProductIdOrderByIsMainDescIdAsc(clientId, productId).stream()
                .map(JpaCardRepository::toDomain)
                .toList();
    }

    private static Card toDomain(CardEntity entity) {
        return new Card(
                entity.getId(),
                entity.getProduct(),
                entity.getMasked(),
                entity.getSubtitle(),
                entity.isMain(),
                entity.getStatus(),
                entity.getBalanceMinorUnits(),
                entity.getIssuedAt(),
                entity.getAccount(),
                entity.getClosedAt());
    }
}
