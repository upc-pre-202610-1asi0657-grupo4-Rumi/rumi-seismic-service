package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.SeismicEventApplicationService;
import com.rumi.seismiccorrelation.application.SeismicFeedUnavailableException;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeismicEventsController.class)
class SeismicEventsControllerTest {

    static final SeismicEvent EVENT = new SeismicEvent(
            "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9", 5.8, "Mw", Instant.parse("2026-10-06T15:29:41Z"),
            -12.05, -77.12, Double.NaN, "Callao, 35 km SW");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeismicEventApplicationService applicationService;

    @Test
    void returnsTheLatestEvent() throws Exception {
        when(applicationService.getLatestEvent()).thenReturn(EVENT);

        mockMvc.perform(get("/api/v1/seismic-events/latest"))
                .andExpect(status().isOk())
                .andExpect(content().json(OpenApiExamples.SEISMIC_EVENT, true));
    }

    @Test
    void returns503WhenTheIgpIsUnavailableAndNothingIsStored() throws Exception {
        when(applicationService.getLatestEvent()).thenThrow(new SeismicFeedUnavailableException());

        mockMvc.perform(get("/api/v1/seismic-events/latest"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Service Unavailable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail")
                        .value("The IGP feed is unavailable and no seismic event is stored yet"));
    }
}
