package com.rumi.seismiccorrelation.infrastructure.web;

import com.rumi.seismiccorrelation.application.ResourceNotFoundException;
import com.rumi.seismiccorrelation.application.RiskIndexQueryService;
import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskIndexesController.class)
class RiskIndexesControllerTest {

    static final UUID BUILDING_ID = UUID.fromString(OpenApiExamples.BUILDING_ID);
    static final UUID RISK_INDEX_ID = UUID.fromString(OpenApiExamples.RISK_INDEX_ID);
    static final CorrelatedSeismicEvent CORRELATION = new CorrelatedSeismicEvent(
            UUID.fromString("9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4"),
            UUID.fromString("1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9"),
            BUILDING_ID,
            EventStatus.ANALYZED,
            Instant.parse("2026-10-06T15:32:05Z"),
            new RiskIndex(RISK_INDEX_ID, RiskLevel.HIGH, Instant.parse("2026-10-06T15:32:10Z"), "rule-based-v1",
                    List.of(new AffectedZoneReport(UUID.randomUUID(), "FLOOR-3-NORTH", 1),
                            new AffectedZoneReport(UUID.randomUUID(), "FLOOR-2-NORTH", 2))));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiskIndexQueryService queryService;

    @Test
    void returnsTheLatestRiskIndexOfTheBuilding() throws Exception {
        when(queryService.getLatestRiskIndex(BUILDING_ID)).thenReturn(CORRELATION);

        mockMvc.perform(get("/api/v1/buildings/{buildingId}/risk-indexes/latest", BUILDING_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(OpenApiExamples.RISK_INDEX, true));
    }

    @Test
    void returns404WhenTheBuildingHasNoRiskIndex() throws Exception {
        UUID unknown = UUID.fromString("0f1e2d3c-4b5a-4968-8776-5a4b3c2d1e0f");
        when(queryService.getLatestRiskIndex(unknown))
                .thenThrow(new ResourceNotFoundException("Building " + unknown + " has no risk index"));

        mockMvc.perform(get("/api/v1/buildings/{buildingId}/risk-indexes/latest", unknown))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Building " + unknown + " has no risk index"));
    }

    @Test
    void returns400WhenTheBuildingIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/building-7/risk-indexes/latest"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("buildingId must be a valid UUID"));
        verifyNoInteractions(queryService);
    }
}
