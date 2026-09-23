package ru.mtsbank.cardoffice.clientidentity.adapters.out.persistence;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * JPA row for the {@code clients} table plus its two ordered scalar
 * collections ({@code client_badges}, {@code client_open_case_ids}) and its
 * child entity collection ({@code client_triggers}) -- schema:
 * db/changelog/changes/001-create-client-identity-schema.xml. Pure
 * persistence mapping; the domain aggregate ({@code Client}) is assembled
 * from this by {@link JpaClientRepository}, never used directly outside
 * this package.
 */
@Entity
@Table(name = "clients")
public class ClientEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "client_since", nullable = false)
    private Integer clientSince;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_badges", joinColumns = @JoinColumn(name = "client_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "badge", nullable = false)
    private List<String> badges = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<ClientTriggerEntity> triggers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_open_case_ids", joinColumns = @JoinColumn(name = "client_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "case_id", nullable = false)
    private List<String> openCaseIds = new ArrayList<>();

    protected ClientEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getClientSince() {
        return clientSince;
    }

    public List<String> getBadges() {
        return badges;
    }

    public List<ClientTriggerEntity> getTriggers() {
        return triggers;
    }

    public List<String> getOpenCaseIds() {
        return openCaseIds;
    }
}
