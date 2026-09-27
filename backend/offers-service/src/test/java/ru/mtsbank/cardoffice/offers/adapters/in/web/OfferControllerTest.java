package ru.mtsbank.cardoffice.offers.adapters.in.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.mtsbank.cardoffice.offers.application.GetTopOfferUseCase;
import ru.mtsbank.cardoffice.offers.domain.Offer;

/**
 * Web-layer test for CAP-2's {@code GET /clients/{clientId}/offers/top}
 * (story 2-offers I/O & Edge-Case Matrix, both rows). Wires
 * {@link OfferController} standalone (plain {@code spring-test} MockMvc, no
 * Spring Boot application context) -- no {@code DataSource}/JPA is ever
 * touched, matching the story's "no JPA, no persistence for this capability"
 * constraint; the port is mocked here, with {@link StubOfferEngineAdapterTest}
 * separately covering the real stub adapter's fixture values.
 */
class OfferControllerTest {

    private static final Offer FIXTURE = new Offer(
            "MTS CASHBACK 111 дней",
            "Повышенный кэшбэк на 111 дней при активации подписки",
            1);

    private final GetTopOfferUseCase getTopOfferUseCase = mock(GetTopOfferUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OfferController(getTopOfferUseCase)).build();
    }

    @Test
    void knownClientReturns200MatchingFixture() throws Exception {
        given(getTopOfferUseCase.execute("client-1")).willReturn(FIXTURE);

        mockMvc.perform(get("/clients/client-1/offers/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(FIXTURE.getTitle()))
                .andExpect(jsonPath("$.text").value(FIXTURE.getText()))
                .andExpect(jsonPath("$.moreCount").value(FIXTURE.getMoreCount()));
    }

    @Test
    void differentClientReturnsSameFixtureUnchanged() throws Exception {
        given(getTopOfferUseCase.execute("client-2")).willReturn(FIXTURE);

        mockMvc.perform(get("/clients/client-2/offers/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(FIXTURE.getTitle()))
                .andExpect(jsonPath("$.text").value(FIXTURE.getText()))
                .andExpect(jsonPath("$.moreCount").value(FIXTURE.getMoreCount()));
    }
}
