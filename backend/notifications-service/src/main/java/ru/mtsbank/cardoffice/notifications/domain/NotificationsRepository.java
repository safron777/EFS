package ru.mtsbank.cardoffice.notifications.domain;

/**
 * Repository port: the domain's view of notifications persistence,
 * implemented by an adapter in {@code adapters/out/persistence} (spine
 * Design Paradigm). The domain depends on this interface only, never on
 * JPA/Spring Data directly.
 *
 * {@code cardId} on {@link #getSummary(String)} is kept as a parameter only
 * for shape consistency with sibling controllers/use-cases (e.g. {@code
 * CashbackController}/{@code GetCashbackUseCase}) -- it is never used to
 * filter or scope the query (story Boundaries & Constraints: one demo-wide
 * fixture, not one row per card). {@link #setChannelState(String, boolean)}
 * never accepts a {@code cardId} at all (story Boundaries & Constraints:
 * toggling never validates or scopes by {@code cardId} -- only {@code
 * notificationId} identifies the target row).
 */
public interface NotificationsRepository {

    NotificationsSummary getSummary(String cardId);

    /**
     * Flips the {@code on} state of the channel identified by {@code
     * notificationId}.
     *
     * @throws NotificationChannelNotFoundException if no channel with that
     *     id exists
     */
    void setChannelState(String notificationId, boolean on);
}
