package ru.mtsbank.cardoffice.cardaccount.domain;

import java.util.Objects;

/**
 * A single account document row as returned to CAP-4 ({@code GET
 * /clients/{clientId}/cards/{cardId}/documents}). Framework-free by design
 * (spine Design Paradigm): no JPA/Spring annotations belong here.
 *
 * Documents are modeled as a global, unscoped list (story Design Notes):
 * {@code mockData.documents} itself is unconditional -- the same three
 * documents regardless of which client or card requested them -- so this
 * entity carries no {@code clientId}/{@code cardId} fields, mirroring
 * {@code Card}'s own choice not to carry query-scoping fields on the domain
 * object (see {@code Card}'s Javadoc for the same rationale).
 */
public final class Document {

    private final String id;
    private final String title;
    private final String subtitle;

    public Document(String id, String title, String subtitle) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.subtitle = Objects.requireNonNull(subtitle, "subtitle");
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
}
