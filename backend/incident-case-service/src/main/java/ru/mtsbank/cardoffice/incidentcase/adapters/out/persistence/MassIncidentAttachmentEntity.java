package ru.mtsbank.cardoffice.incidentcase.adapters.out.persistence;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code mass_incident_attachments} table -- schema:
 * db/changelog/changes/001-create-incident-case-schema.xml. Pure persistence
 * mapping; never used directly outside this package.
 *
 * Purely a link row with no identity of its own beyond the {@code
 * (incident_id, client_id)} pair it records -- the same precedent as {@code
 * client-identity-service}'s {@code client_badges}/{@code
 * client_open_case_ids} (composite primary key, no surrogate id column),
 * rather than {@code client_triggers}' surrogate-id-plus-unique-constraint
 * shape. The composite primary key both is the uniqueness guard the task
 * calls for ("unique on (incident_id, client_id)") and makes a repeat
 * attach of the same pair a plain {@code save} of an entity Hibernate
 * already knows exists -- no separate existence check needed to stay
 * idempotent, see {@link JpaMassIncidentRepository#attachClient}.
 */
@Entity
@Table(name = "mass_incident_attachments")
@IdClass(MassIncidentAttachmentEntity.Key.class)
public class MassIncidentAttachmentEntity {

    @Id
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @Id
    @Column(name = "client_id", length = 64)
    private String clientId;

    @Column(name = "attached_at", nullable = false)
    private Instant attachedAt;

    protected MassIncidentAttachmentEntity() {
        // JPA
    }

    public MassIncidentAttachmentEntity(String incidentId, String clientId, Instant attachedAt) {
        this.incidentId = incidentId;
        this.clientId = clientId;
        this.attachedAt = attachedAt;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getClientId() {
        return clientId;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    /** {@link IdClass} companion for the {@code (incidentId, clientId)} composite key. */
    public static final class Key implements Serializable {

        private String incidentId;
        private String clientId;

        public Key() {
            // JPA
        }

        public Key(String incidentId, String clientId) {
            this.incidentId = incidentId;
            this.clientId = clientId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(incidentId, key.incidentId) && Objects.equals(clientId, key.clientId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(incidentId, clientId);
        }
    }
}
