package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.InvalidQueryException;
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
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void listsTheStoredEventsInTheRange() throws Exception {
        when(applicationService.listEvents(
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-07T00:00:00Z")))
                .thenReturn(List.of(EVENT));

        mockMvc.perform(get("/api/v1/seismic-events")
                        .param("from", "2026-10-01T00:00:00Z")
                        .param("to", "2026-10-07T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9"))
                .andExpect(jsonPath("$[0].magnitude.scale").value("Mw"));
    }

    @Test
    void listsEveryStoredEventWithoutBounds() throws Exception {
        when(applicationService.listEvents(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/seismic-events"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returns400WhenADateIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/seismic-events").param("from", "06/10/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("from must be an ISO-8601 instant, for example 2026-10-06T15:30:00Z"));
        verifyNoInteractions(applicationService);
    }

    @Test
    void returns400WhenTheRangeIsReversed() throws Exception {
        when(applicationService.listEvents(
                Instant.parse("2026-10-07T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z")))
                .thenThrow(new InvalidQueryException("from must not be after to"));

        mockMvc.perform(get("/api/v1/seismic-events")
                        .param("from", "2026-10-07T00:00:00Z")
                        .param("to", "2026-10-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("from must not be after to"));
    }
}
