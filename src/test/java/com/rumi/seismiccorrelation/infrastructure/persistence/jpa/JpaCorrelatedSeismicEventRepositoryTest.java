package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaSeismicEventRepository.class, JpaCorrelatedSeismicEventRepository.class})
class JpaCorrelatedSeismicEventRepositoryTest {

    static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");

    @Autowired
    private JpaSeismicEventRepository seismicEventRepository;

    @Autowired
    private JpaCorrelatedSeismicEventRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID seismicEventId;

    @BeforeEach
    void storeSeismicEvent() {
        seismicEventId = UUID.fromString(seismicEventRepository.save(new SeismicEvent(
                "IGP-2026-0412", 5.8, Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 40.0)).getId());
    }

    @Test
    void storesADetectedCorrelationWithoutRiskIndex() {
        CorrelatedSeismicEvent detected = CorrelatedSeismicEvent.detect(
                seismicEventId, BUILDING_ID, Instant.parse("2026-10-06T15:31:00Z"));

        repository.save(detected);
        entityManager.flush();
        entityManager.clear();

        CorrelatedSeismicEvent found = repository.findById(detected.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EventStatus.DETECTED);
        assertThat(found.getSeismicEventId()).isEqualTo(seismicEventId);
        assertThat(found.getBuildingId()).isEqualTo(BUILDING_ID);
        assertThat(found.getRiskIndex()).isEmpty();
    }

    @Test
    void storesTheRiskIndexAndItsAffectedZonesOrderedBySeverity() {
        CorrelatedSeismicEvent correlation = CorrelatedSeismicEvent.detect(
                seismicEventId, BUILDING_ID, Instant.parse("2026-10-06T15:31:00Z"));
        repository.save(correlation);
        entityManager.flush();
        entityManager.clear();
        correlation.analyze(new RiskIndex(
                UUID.randomUUID(), RiskLevel.HIGH, Instant.parse("2026-10-06T15:32:10Z"), "rule-based-v1",
                List.of(new AffectedZoneReport(UUID.randomUUID(), "FLOOR-2-NORTH", 2),
                        new AffectedZoneReport(UUID.randomUUID(), "FLOOR-3-NORTH", 1))));

        repository.save(correlation);
        entityManager.flush();
        entityManager.clear();

        CorrelatedSeismicEvent found = repository.findById(correlation.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(EventStatus.ANALYZED);
        RiskIndex riskIndex = found.getRiskIndex().orElseThrow();
        assertThat(riskIndex.getLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(riskIndex.getModelVersion()).isEqualTo("rule-based-v1");
        assertThat(riskIndex.getCalculatedAt()).isEqualTo(Instant.parse("2026-10-06T15:32:10Z"));
        assertThat(riskIndex.getAffectedZones()).extracting(AffectedZoneReport::zone)
                .containsExactly("FLOOR-3-NORTH", "FLOOR-2-NORTH");
    }
}
