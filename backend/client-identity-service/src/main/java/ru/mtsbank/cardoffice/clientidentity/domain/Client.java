package ru.mtsbank.cardoffice.clientidentity.domain;

import java.util.List;
import java.util.Objects;

/**
 * Client identity aggregate: profile, badges, triggers and reference-only
 * open-case ids. Framework-free by design (spine Design Paradigm): no
 * JPA/Spring annotations belong here.
 *
 * {@code openCaseIds} is bare case ids only, never enriched with the
 * human-readable label {@code incident-case-service} owns (AD-2: cross-context
 * references are by id only -- see story 1-client-identity Boundaries &
 * Constraints for the deliberate divergence from the frontend mock's
 * {@code openCases: [{id, label}]} shape).
 */
public final class Client {

    private final String id;
    private final String fullName;
    private final int clientSince;
    private final List<String> badges;
    private final List<ClientTrigger> triggers;
    private final List<String> openCaseIds;

    public Client(
            String id,
            String fullName,
            int clientSince,
            List<String> badges,
            List<ClientTrigger> triggers,
            List<String> openCaseIds) {
        this.id = Objects.requireNonNull(id, "id");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.clientSince = clientSince;
        this.badges = List.copyOf(Objects.requireNonNull(badges, "badges"));
        this.triggers = List.copyOf(Objects.requireNonNull(triggers, "triggers"));
        this.openCaseIds = List.copyOf(Objects.requireNonNull(openCaseIds, "openCaseIds"));
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public int getClientSince() {
        return clientSince;
    }

    public List<String> getBadges() {
        return badges;
    }

    public List<ClientTrigger> getTriggers() {
        return triggers;
    }

    public List<String> getOpenCaseIds() {
        return openCaseIds;
    }
}
