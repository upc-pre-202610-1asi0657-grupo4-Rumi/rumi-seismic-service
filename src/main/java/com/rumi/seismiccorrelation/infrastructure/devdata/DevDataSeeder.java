package com.rumi.seismiccorrelation.infrastructure.devdata;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.domain.strategy.RuleBasedRiskStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sample data of the dev profile, with the ids used in the OpenAPI examples: one seismic event,
 * its correlation with the sample building, a HIGH risk index and two affected zones.
 * Disabled with rumi.dev.sample-data=false.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "rumi.dev.sample-data", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements ApplicationRunner {

    public static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    public static final UUID SEISMIC_EVENT_ID = UUID.fromString("1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9");
    public static final UUID CORRELATED_EVENT_ID = UUID.fromString("9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4");
    public static final UUID RISK_INDEX_ID = UUID.fromString("c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a");

    private static final Logger LOGGER = LoggerFactory.getLogger(DevDataSeeder.class);

    private final SeismicEventRepository seismicEventRepository;
    private final CorrelatedSeismicEventRepository correlatedEventRepository;

    public DevDataSeeder(
            SeismicEventRepository seismicEventRepository,
            CorrelatedSeismicEventRepository correlatedEventRepository
    ) {
        this.seismicEventRepository = seismicEventRepository;
        this.correlatedEventRepository = correlatedEventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seismicEventRepository.findById(SEISMIC_EVENT_ID).isPresent()) {
            return;
        }
        seismicEventRepository.save(new SeismicEvent(
                SEISMIC_EVENT_ID.toString(),
                5.8,
                "Mw",
                Instant.parse("2026-10-06T15:29:41Z"),
                -12.05,
                -77.12,
                38.0,
                "Callao, 35 km SW"
        ));
        correlatedEventRepository.save(new CorrelatedSeismicEvent(
                CORRELATED_EVENT_ID,
                SEISMIC_EVENT_ID,
                BUILDING_ID,
                EventStatus.ANALYZED,
                Instant.parse("2026-10-06T15:32:05Z"),
                new RiskIndex(
                        RISK_INDEX_ID,
                        RiskLevel.HIGH,
                        Instant.parse("2026-10-06T15:32:10Z"),
                        RuleBasedRiskStrategy.MODEL_VERSION,
                        List.of(
                                new AffectedZoneReport(
                                        UUID.fromString("4f5e6d7c-8b9a-4c0d-9e1f-2a3b4c5d6e7f"), "FLOOR-3-NORTH", 1),
                                new AffectedZoneReport(
                                        UUID.fromString("5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d"), "FLOOR-2-NORTH", 2)
                        )
                )
        ));
        LOGGER.info("Dev sample data loaded for building {}", BUILDING_ID);
    }
}
