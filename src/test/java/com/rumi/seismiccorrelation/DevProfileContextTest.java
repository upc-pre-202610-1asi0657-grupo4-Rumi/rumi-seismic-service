package com.rumi.seismiccorrelation;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import com.rumi.seismiccorrelation.domain.repository.SeismicEventRepository;
import com.rumi.seismiccorrelation.infrastructure.devdata.DevDataSeeder;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpFeedOperation;
import com.rumi.seismiccorrelation.infrastructure.external.igp.IgpSeismicEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileContextTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SeismicEventRepository seismicEventRepository;

    @Autowired
    private CorrelatedSeismicEventRepository correlatedEventRepository;

    @Autowired
    private IgpFeedOperation igpFeedOperation;

    @Test
    void runsOnAnInMemoryDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
        }
    }

    @Test
    void loadsTheSampleDataOfTheSampleBuilding() {
        SeismicEvent event = seismicEventRepository.findById(DevDataSeeder.SEISMIC_EVENT_ID).orElseThrow();
        assertThat(event.getEpicenterDescription()).isEqualTo("Callao, 35 km SW");

        CorrelatedSeismicEvent correlation =
                correlatedEventRepository.findById(DevDataSeeder.CORRELATED_EVENT_ID).orElseThrow();
        assertThat(correlation.getBuildingId()).isEqualTo(DevDataSeeder.BUILDING_ID);
        assertThat(correlation.getRiskIndex()).hasValueSatisfying(riskIndex -> {
            assertThat(riskIndex.getId()).isEqualTo(DevDataSeeder.RISK_INDEX_ID);
            assertThat(riskIndex.getLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(riskIndex.getAffectedZones()).extracting(AffectedZoneReport::zone)
                    .containsExactly("FLOOR-3-NORTH", "FLOOR-2-NORTH");
        });
    }

    @Test
    void theStubFeedReportsTheSampleEvent() {
        SeismicEvent fromFeed = new IgpSeismicEventMapper().toDomain(igpFeedOperation.fetchLatest());
        SeismicEvent seeded = seismicEventRepository.findById(DevDataSeeder.SEISMIC_EVENT_ID).orElseThrow();

        assertThat(fromFeed.isSameOccurrenceAs(seeded)).isTrue();
    }
}
