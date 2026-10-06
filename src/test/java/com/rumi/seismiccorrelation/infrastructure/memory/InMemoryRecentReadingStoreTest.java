package com.rumi.seismiccorrelation.infrastructure.memory;

import com.rumi.seismiccorrelation.domain.model.RecentReading;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRecentReadingStoreTest {

    static final Instant NOW = Instant.parse("2026-10-06T15:35:00Z");
    static final UUID BUILDING_ID = UUID.fromString("7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d");

    private final InMemoryRecentReadingStore store =
            new InMemoryRecentReadingStore(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(30), 3);

    @Test
    void findsTheReadingsOfABuildingInAnInclusiveRange() {
        store.add(reading(BUILDING_ID, "2026-10-06T15:29:41Z"));
        store.add(reading(BUILDING_ID, "2026-10-06T15:32:00Z"));
        store.add(reading(BUILDING_ID, "2026-10-06T15:34:42Z"));
        store.add(reading(UUID.randomUUID(), "2026-10-06T15:30:00Z"));

        assertThat(store.findBetween(BUILDING_ID,
                Instant.parse("2026-10-06T15:29:41Z"), Instant.parse("2026-10-06T15:32:00Z")))
                .extracting(RecentReading::recordedAt)
                .containsExactly(Instant.parse("2026-10-06T15:29:41Z"), Instant.parse("2026-10-06T15:32:00Z"));
    }

    @Test
    void forgetsReadingsOlderThanTheRetention() {
        store.add(reading(BUILDING_ID, "2026-10-06T15:04:59Z"));
        store.add(reading(BUILDING_ID, "2026-10-06T15:05:00Z"));

        assertThat(store.findBetween(BUILDING_ID, Instant.EPOCH, NOW)).extracting(RecentReading::recordedAt)
                .containsExactly(Instant.parse("2026-10-06T15:05:00Z"));
    }

    @Test
    void keepsAtMostTheConfiguredNumberOfReadingsPerBuilding() {
        for (int second = 0; second < 5; second++) {
            store.add(reading(BUILDING_ID, "2026-10-06T15:30:0" + second + "Z"));
        }

        assertThat(store.findBetween(BUILDING_ID, Instant.EPOCH, NOW)).hasSize(3);
    }

    @Test
    void listsTheBuildingsWithReadingsInARange() {
        UUID otherBuilding = UUID.randomUUID();
        store.add(reading(BUILDING_ID, "2026-10-06T15:30:00Z"));
        store.add(reading(otherBuilding, "2026-10-06T15:20:00Z"));

        assertThat(store.findBuildingsWithReadingsBetween(
                Instant.parse("2026-10-06T15:29:41Z"), Instant.parse("2026-10-06T15:34:41Z")))
                .containsExactly(BUILDING_ID);
    }

    private static RecentReading reading(UUID buildingId, String recordedAt) {
        return new RecentReading(buildingId, "FLOOR-3-NORTH", Instant.parse(recordedAt), 0.42);
    }
}
