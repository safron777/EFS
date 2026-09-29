package ru.mtsbank.cardoffice.cardaccount.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code documents} table -- schema:
 * db/changelog/changes/003-create-documents-and-seed.xml. Pure persistence
 * mapping; the domain object ({@code Document}) is assembled from this by
 * {@link JpaDocumentRepository}, never used directly outside this package.
 *
 * No {@code client_id}/{@code card_id} columns -- documents are global in
 * this demo (story Design Notes), unlike {@code CardEntity}'s
 * {@code client_id}/{@code product_id} scoping columns.
 *
 * {@code sortOrder} exists only to reproduce {@code mockData.documents}'
 * array order (statement, balance certificate, flow certificate) exactly --
 * that order isn't alphabetical by {@code id} and isn't otherwise derivable
 * from the row's own fields, so an explicit ordering column is needed (the
 * same problem {@code CardEntity} solved differently, by sorting on the
 * meaningful {@code is_main} flag it already has).
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", nullable = false, length = 255)
    private String subtitle;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected DocumentEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
