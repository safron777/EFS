package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.mtsbank.cardoffice.cardaccount.domain.Document;
import ru.mtsbank.cardoffice.cardaccount.domain.DocumentRepository;

/**
 * Implements the domain {@link DocumentRepository} port against this
 * service's own PostgreSQL schema (AD-1: DB-per-service). The only place
 * that translates between {@link DocumentEntity} (JPA) and {@link Document}
 * (domain).
 */
@Repository
public class JpaDocumentRepository implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;

    public JpaDocumentRepository(DocumentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return jpaRepository.findAllByOrderBySortOrderAsc().stream()
                .map(JpaDocumentRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    private static Document toDomain(DocumentEntity entity) {
        return new Document(entity.getId(), entity.getTitle(), entity.getSubtitle());
    }
}
