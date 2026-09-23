package ru.mtsbank.cardoffice.clientidentity.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One profile alert/trigger (e.g. "passport expired", "VIP client") shown on
 * the client card. Framework-free by design (spine Design Paradigm): no
 * JPA/Spring annotations belong here.
 */
public final class ClientTrigger {

    /**
     * AD-4: a link field must be a relative-internal path, or null -- never
     * an absolute/external/protocol-relative URL. Mirrors the frontend's own
     * {@code IdentityBar.isSafeInternalLink} guard (GHSA-wrjc-x8rr-h8h6) so
     * this service does not rely on the frontend as the only enforcement
     * point, even though today's seed data is trusted.
     */
    private static final Pattern SAFE_INTERNAL_LINK = Pattern.compile("^/(?!/|\\\\).*");

    private final String id;
    private final String icon;
    private final String tone;
    private final String title;
    private final String text;
    private final String link;

    public ClientTrigger(String id, String icon, String tone, String title, String text, String link) {
        this.id = Objects.requireNonNull(id, "id");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.tone = Objects.requireNonNull(tone, "tone");
        this.title = Objects.requireNonNull(title, "title");
        this.text = Objects.requireNonNull(text, "text");
        this.link = validateLink(link);
    }

    private static String validateLink(String link) {
        if (link == null) {
            return null;
        }
        if (!SAFE_INTERNAL_LINK.matcher(link).matches()) {
            throw new IllegalStateException(
                    "trigger link must be a relative-internal path or null (AD-4), got: " + link);
        }
        return link;
    }

    public String getId() {
        return id;
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
