package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.event.SensorReadingRecorded;
import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RecentReading;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import com.rumi.seismiccorrelation.domain.repository.RecentReadingStore;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.domain.service.RiskIndexCalculationService;
import com.rumi.seismiccorrelation.domain.service.SeismicCorrelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Correlates the readings of each building with the IGP events. The readings usually arrive
 * before the IGP publishes the event, so the correlation is attempted from both sides: when a
 * reading arrives and when a new event is stored. Each event is correlated once per building,
 * with the readings available at that moment.
 */
@Service
public class SeismicCorrelationApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeismicCorrelationApplicationService.class);

    private final RecentReadingStore recentReadingStore;
    private final SeismicEventRepository seismicEventRepository;
    private final CorrelatedSeismicEventRepository correlatedEventRepository;
    private final SeismicCorrelationService correlationService;
    private final RiskIndexCalculationService riskIndexCalculationService;
    private final Clock clock;

    public SeismicCorrelationApplicationService(
            RecentReadingStore recentReadingStore,
            SeismicEventRepository seismicEventRepository,
            CorrelatedSeismicEventRepository correlatedEventRepository,
            SeismicCorrelationService correlationService,
            RiskIndexCalculationService riskIndexCalculationService,
            Clock clock
    ) {
        this.recentReadingStore = recentReadingStore;
        this.seismicEventRepository = seismicEventRepository;
        this.correlatedEventRepository = correlatedEventRepository;
        this.correlationService = correlationService;
        this.riskIndexCalculationService = riskIndexCalculationService;
        this.clock = clock;
    }

    /** Keeps the reading and correlates it with the stored events whose window contains it. */
    public synchronized List<CorrelatedSeismicEvent> recordReading(SensorReadingRecorded event) {
        recentReadingStore.add(new RecentReading(event.buildingId(), event.zone(), event.recordedAt(), event.value()));
        List<CorrelatedSeismicEvent> correlations = new ArrayList<>();
        seismicEventRepository
                .findOccurredBetween(event.recordedAt().minus(correlationService.window()), event.recordedAt())
                .forEach(seismicEvent -> correlate(seismicEvent, event.buildingId()).ifPresent(correlations::add));
        return correlations;
    }

    /** Correlates a newly stored IGP event with every building that has readings in its window. */
    public synchronized List<CorrelatedSeismicEvent> processOfficialEvent(SeismicEvent storedEvent) {
        List<CorrelatedSeismicEvent> correlations = new ArrayList<>();
        recentReadingStore
                .findBuildingsWithReadingsBetween(
                        correlationService.windowStart(storedEvent), correlationService.windowEnd(storedEvent))
                .forEach(buildingId -> correlate(storedEvent, buildingId).ifPresent(correlations::add));
        return correlations;
    }

    private Optional<CorrelatedSeismicEvent> correlate(SeismicEvent seismicEvent, UUID buildingId) {
        UUID seismicEventId = UUID.fromString(seismicEvent.getId());
        if (correlatedEventRepository.existsBySeismicEventIdAndBuildingId(seismicEventId, buildingId)) {
            return Optional.empty();
        }
        List<RecentReading> readings = recentReadingStore.findBetween(
                buildingId, correlationService.windowStart(seismicEvent), correlationService.windowEnd(seismicEvent));
        if (readings.isEmpty()) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        CorrelatedSeismicEvent correlation = correlationService.correlate(seismicEvent, buildingId, now);
        correlation.analyze(calculateRiskIndex(readings, now));
        CorrelatedSeismicEvent saved = correlatedEventRepository.save(correlation);
        LOGGER.info("Seismic event {} correlated with building {}: risk {}",
                seismicEventId, buildingId, saved.getRiskIndex().map(RiskIndex::getLevel).orElse(null));
        return Optional.of(saved);
    }

    /**
     * The level of the building is the level of its most affected zone. Zones whose own level is
     * MEDIUM or higher become affected zones, ranked from the highest response.
     */
    private RiskIndex calculateRiskIndex(List<RecentReading> readings, Instant calculatedAt) {
        Map<String, Double> responseByZone = correlationService.normalizedResponseByZone(readings);
        double buildingResponse = Collections.max(responseByZone.values());
        RiskLevel level = riskIndexCalculationService.calculateLevel(buildingResponse);
        List<AffectedZoneReport> affectedZones = new ArrayList<>();
        if (level.isAtLeast(RiskLevel.MEDIUM)) {
            AtomicInteger rank = new AtomicInteger();
            responseByZone.entrySet().stream()
                    .filter(zone -> riskIndexCalculationService.calculateLevel(zone.getValue())
                            .isAtLeast(RiskLevel.MEDIUM))
                    .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .forEach(zone -> affectedZones.add(
                            new AffectedZoneReport(UUID.randomUUID(), zone.getKey(), rank.incrementAndGet())));
        }
        return new RiskIndex(
                UUID.randomUUID(), level, calculatedAt, riskIndexCalculationService.modelVersion(), affectedZones);
    }
}
