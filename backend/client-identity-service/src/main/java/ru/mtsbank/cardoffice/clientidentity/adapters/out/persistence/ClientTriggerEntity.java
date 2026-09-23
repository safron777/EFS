package ru.mtsbank.cardoffice.clientidentity.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code client_triggers} table (schema:
 * db/changelog/changes/001-create-client-identity-schema.xml). Pure
 * persistence mapping -- no business logic, no AD-4 validation here; that
 * lives once, in the domain's {@code ClientTrigger} constructor.
 */
@Entity
@Table(name = "client_triggers")
public class ClientTriggerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "trigger_key", nullable = false)
    private String triggerKey;

    @Column(name = "icon", nullable = false)
    private String icon;

    @Column(name = "tone", nullable = false)
    private String tone;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "link")
    private String link;

    protected ClientTriggerEntity() {
        // JPA
    }

    public Long getId() {
        return id;
    }

    public String getTriggerKey() {
        return triggerKey;
    }

    public String getIcon() {
        return icon;
    }

    public String getTone() {
        return tone;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getLink() {
        return link;
    }
}
