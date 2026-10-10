package ru.mtsbank.cardoffice.cardissuance.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceApplication;
import ru.mtsbank.cardoffice.cardissuance.domain.IssuanceRepository;

/**
 * Implements the domain {@link IssuanceRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link IssuanceApplicationEntity} (JPA) and
 * {@link IssuanceApplication} (domain).
 *
 * {@link #save(IssuanceApplication)} assigns the next {@code sort_order} as
 * one past the current maximum, so a newly created application is always
 * appended after the seeded/existing rows (story Boundaries & Constraints:
 * "a follow-up GET shows it appended to history"), robust even if rows are
 * ever removed (unlike a plain row-count scheme).
 */
@Repository
public class JpaIssuanceRepository implements IssuanceRepository {

    private final IssuanceApplicationJpaRepository jpaRepository;

    public JpaIssuanceRepository(IssuanceApplicationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssuanceApplication> findAllOrdered() {
        return jpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(JpaIssuanceRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public IssuanceApplication save(IssuanceApplication application) {
        int nextSortOrder = jpaRepository.findMaxSortOrder() + 1;
        IssuanceApplicationEntity entity = new IssuanceApplicationEntity(
                application.getId(),
                application.getDate(),
                application.getCard(),
                application.getMethod(),
                application.getStatus(),
                nextSortOrder);
        IssuanceApplicationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    private static IssuanceApplication toDomain(IssuanceApplicationEntity entity) {
        return new IssuanceApplication(
                entity.getId(), entity.getDate(), entity.getCard(), entity.getMethod(), entity.getStatus());
    }
}
