package ru.mtsbank.cardoffice.clientidentity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/**
 * Plain-Java unit test for the AD-4 link-safety guard in
 * {@link ClientTrigger}'s constructor -- no Spring/Testcontainers needed. All
 * seeded links in mockData.client are already safe, so without this test a
 * regression that weakened {@code validateLink} would ship undetected behind
 * a fully green {@code ClientControllerTest} suite.
 */
class ClientTriggerTest {

    @ParameterizedTest
    @ValueSource(strings = {"//evil.com", "\\evil.com", "http://evil.com", "https://evil.com", "javascript:alert(1)"})
    void rejectsUnsafeLinks(String unsafeLink) {
        assertThatThrownBy(() -> newTrigger(unsafeLink))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AD-4");
    }

    @Test
    void acceptsRelativeInternalLink() {
        ClientTrigger trigger = newTrigger("/client/data");

        assertThat(trigger.getLink()).isEqualTo("/client/data");
    }

    @Test
    void acceptsNullLink() {
        ClientTrigger trigger = newTrigger(null);

        assertThat(trigger.getLink()).isNull();
    }

    private static ClientTrigger newTrigger(String link) {
        return new ClientTrigger("id", "icon", "tone", "title", "text", link);
    }
}
