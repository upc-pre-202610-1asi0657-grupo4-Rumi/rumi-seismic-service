package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.domain.service.RiskIndexCalculationService;
import com.rumi.seismiccorrelation.domain.service.SeismicCorrelationService;
import com.rumi.seismiccorrelation.domain.strategy.RuleBasedRiskStrategy;
import com.rumi.seismiccorrelation.infrastructure.memory.InMemoryRecentReadingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeismicCorrelationApplicationServiceTest {

    static final Instant NOW = Instant.parse("2026-10-06T15:36:00Z");
    static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    static final UUID SENSOR_ID = UUID.fromString("5d1c2f0e-8f4a-4a53-9a7e-0f3b1c9d7a11");
    static final SeismicEvent EVENT = new SeismicEvent(
            "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9", 5.8, Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 38.0);
    static final UUID EVENT_ID = UUID.fromString(EVENT.getId());

    private SeismicEventRepository seismicEventRepository;
    private CorrelatedSeismicEventRepository correlatedEventRepository;
    private SeismicCorrelationApplicationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        seismicEventRepository = mock(SeismicEventRepository.class);
        correlatedEventRepository = mock(CorrelatedSeismicEventRepository.class);
        when(correlatedEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new SeismicCorrelationApplicationService(
                new InMemoryRecentReadingStore(clock, Duration.ofMinutes(30), 10_000),
                seismicEventRepository,
                correlatedEventRepository,
                new SeismicCorrelationService(Duration.ofMinutes(5), 1.0),
                new RiskIndexCalculationService(new RuleBasedRiskStrategy()),
                clock
        );
    }

    @Test
    void correlatesAReadingWithAStoredEventWhoseWindowContainsIt() {
        when(seismicEventRepository.findOccurredBetween(
                Instant.parse("2026-10-06T15:25:30Z"), Instant.parse("2026-10-06T15:30:30Z")))
                .thenReturn(List.of(EVENT));

        List<CorrelatedSeismicEvent> correlations = service.recordReading(reading("FLOOR-3-NORTH", "15:30:30", 0.8));

        assertThat(correlations).singleElement().satisfies(correlation -> {
            assertThat(correlation.getSeismicEventId()).isEqualTo(EVENT_ID);
            assertThat(correlation.getBuildingId()).isEqualTo(BUILDING_ID);
            assertThat(correlation.getStatus()).isEqualTo(EventStatus.ANALYZED);
            assertThat(correlation.getCorrelatedAt()).isEqualTo(NOW);
            RiskIndex riskIndex = correlation.getRiskIndex().orElseThrow();
            assertThat(riskIndex.getLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(riskIndex.getModelVersion()).isEqualTo("rule-based-v1");
            assertThat(riskIndex.getCalculatedAt()).isEqualTo(NOW);
            assertThat(riskIndex.getAffectedZones()).extracting(AffectedZoneReport::zone)
                    .containsExactly("FLOOR-3-NORTH");
        });
        verify(correlatedEventRepository).save(correlations.get(0));
    }

    @Test
    void doesNotCorrelateAReadingWithoutAnEventInItsWindow() {
        when(seismicEventRepository.findOccurredBetween(any(), any())).thenReturn(List.of());

        assertThat(service.recordReading(reading("FLOOR-3-NORTH", "15:30:30", 0.8))).isEmpty();
        verify(correlatedEventRepository, never()).save(any());
    }

    @Test
    void correlatesEachEventOnlyOncePerBuilding() {
        when(seismicEventRepository.findOccurredBetween(any(), any())).thenReturn(List.of(EVENT));
        when(correlatedEventRepository.existsBySeismicEventIdAndBuildingId(EVENT_ID, BUILDING_ID)).thenReturn(true);

        assertThat(service.recordReading(reading("FLOOR-3-NORTH", "15:30:30", 0.8))).isEmpty();
        verify(correlatedEventRepository, never()).save(any());
    }

    @Test
    void correlatesANewEventWithTheReadingsReceivedBeforeIt() {
        when(seismicEventRepository.findOccurredBetween(any(), any())).thenReturn(List.of());
        service.recordReading(reading("FLOOR-3-NORTH", "15:30:10", 0.80));
        service.recordReading(reading("FLOOR-2-NORTH", "15:30:20", 0.50));
        service.recordReading(reading("FLOOR-1-SOUTH", "15:30:30", 0.10));
        service.recordReading(reading("FLOOR-4-NORTH", "15:28:00", 0.95));

        List<CorrelatedSeismicEvent> correlations = service.processOfficialEvent(EVENT);

        RiskIndex riskIndex = correlations.get(0).getRiskIndex().orElseThrow();
        assertThat(riskIndex.getLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(riskIndex.getAffectedZones())
                .extracting(AffectedZoneReport::zone, AffectedZoneReport::severityRank)
                .containsExactly(
                        tuple("FLOOR-3-NORTH", 1),
                        tuple("FLOOR-2-NORTH", 2));
    }

    @Test
    void reportsNoAffectedZonesBelowMedium() {
        when(seismicEventRepository.findOccurredBetween(any(), any())).thenReturn(List.of(EVENT));

        List<CorrelatedSeismicEvent> correlations = service.recordReading(reading("FLOOR-3-NORTH", "15:30:30", 0.1));

        RiskIndex riskIndex = correlations.get(0).getRiskIndex().orElseThrow();
        assertThat(riskIndex.getLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskIndex.getAffectedZones()).isEmpty();
    }

    @Test
    void doesNotCorrelateANewEventWithoutReadingsInItsWindow() {
        when(seismicEventRepository.findOccurredBetween(any(), any())).thenReturn(List.of());
        service.recordReading(reading("FLOOR-3-NORTH", "15:35:00", 0.8));

        assertThat(service.processOfficialEvent(EVENT)).isEmpty();
    }

    private static SensorReadingRecorded reading(String zone, String time, double vibration) {
        return new SensorReadingRecorded(
                UUID.randomUUID(), SENSOR_ID, BUILDING_ID, zone, Instant.parse("2026-10-06T" + time + "Z"), vibration);
    }
}
