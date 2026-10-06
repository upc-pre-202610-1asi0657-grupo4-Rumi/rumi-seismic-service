package com.rumi.seismiccorrelation.domain.service;

import com.rumi.seismiccorrelation.domain.model.EventStatus;
import com.rumi.seismiccorrelation.domain.model.RecentReading;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class SeismicCorrelationServiceTest {

    static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");
    static final SeismicEvent EVENT = new SeismicEvent(
            "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9", 5.8, Instant.parse("2026-10-06T15:29:41Z"), -12.05, -77.12, 38.0);

    private final SeismicCorrelationService service = new SeismicCorrelationService(Duration.ofMinutes(5), 0.5);

    @Test
    void theWindowStartsAtTheOccurrenceOfTheEvent() {
        assertThat(service.windowStart(EVENT)).isEqualTo(Instant.parse("2026-10-06T15:29:41Z"));
        assertThat(service.windowEnd(EVENT)).isEqualTo(Instant.parse("2026-10-06T15:34:41Z"));
    }

    @Test
    void createsADetectedCorrelation() {
        var correlation = service.correlate(EVENT, BUILDING_ID, Instant.parse("2026-10-06T15:32:00Z"));

        assertThat(correlation.getStatus()).isEqualTo(EventStatus.DETECTED);
        assertThat(correlation.getSeismicEventId()).isEqualTo(UUID.fromString(EVENT.getId()));
        assertThat(correlation.getBuildingId()).isEqualTo(BUILDING_ID);
    }

    @Test
    void normalizesThePeakVibrationOfEachZoneAndCapsItAtOne() {
        var response = service.normalizedResponseByZone(List.of(
                reading("FLOOR-3-NORTH", 0.10),
                reading("FLOOR-3-NORTH", -0.40),
                reading("FLOOR-2-NORTH", 0.20),
                reading("FLOOR-1-SOUTH", 0.90)
        ));

        assertThat(response).containsOnlyKeys("FLOOR-1-SOUTH", "FLOOR-2-NORTH", "FLOOR-3-NORTH");
        assertThat(response.get("FLOOR-3-NORTH")).isCloseTo(0.8, offset(1e-9));
        assertThat(response.get("FLOOR-2-NORTH")).isCloseTo(0.4, offset(1e-9));
        assertThat(response.get("FLOOR-1-SOUTH")).isEqualTo(1.0);
    }

    @Test
    void rejectsAnInvalidConfiguration() {
        assertThatThrownBy(() -> new SeismicCorrelationService(Duration.ZERO, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeismicCorrelationService(Duration.ofMinutes(5), 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static RecentReading reading(String zone, double vibration) {
        return new RecentReading(BUILDING_ID, zone, Instant.parse("2026-10-06T15:30:00Z"), vibration);
    }
}
