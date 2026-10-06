package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskIndexQueryServiceTest {

    static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    static final CorrelatedSeismicEvent CORRELATION = new CorrelatedSeismicEvent(
            UUID.fromString("9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4"),
            UUID.fromString("1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9"),
            BUILDING_ID,
            EventStatus.ANALYZED,
            Instant.parse("2026-10-06T15:32:05Z"),
            new RiskIndex(UUID.fromString("c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a"), RiskLevel.HIGH,
                    Instant.parse("2026-10-06T15:32:10Z"), "rule-based-v1", List.of()));

    private CorrelatedSeismicEventRepository repository;
    private RiskIndexQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CorrelatedSeismicEventRepository.class);
        service = new RiskIndexQueryService(repository);
    }

    @Test
    void returnsTheLatestRiskIndexOfTheBuilding() {
        when(repository.findLatestWithRiskIndexByBuildingId(BUILDING_ID)).thenReturn(Optional.of(CORRELATION));

        assertThat(service.getLatestRiskIndex(BUILDING_ID)).isSameAs(CORRELATION);
    }

    @Test
    void failsWhenTheBuildingHasNoRiskIndex() {
        when(repository.findLatestWithRiskIndexByBuildingId(BUILDING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestRiskIndex(BUILDING_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Building 7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d has no risk index");
    }
}
