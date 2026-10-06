package com.rumi.seismiccorrelation.infrastructure.memory;

import com.rumi.seismiccorrelation.domain.model.RecentReading;
import com.rumi.seismiccorrelation.domain.repository.RecentReadingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the readings of the last {@code retention} in memory, at most {@code maxPerBuilding} per
 * building. The data is lost on restart and is not shared between instances.
 */
@Component
public class InMemoryRecentReadingStore implements RecentReadingStore {

    private final Clock clock;
    private final Duration retention;
    private final int maxPerBuilding;
    private final Map<UUID, Deque<RecentReading>> readingsByBuilding = new HashMap<>();

    public InMemoryRecentReadingStore(
            Clock clock,
            @Value("${rumi.correlation.retention:30m}") Duration retention,
            @Value("${rumi.correlation.max-readings-per-building:10000}") int maxPerBuilding
    ) {
        this.clock = clock;
        this.retention = retention;
        this.maxPerBuilding = maxPerBuilding;
    }

    @Override
    public synchronized void add(RecentReading reading) {
        Deque<RecentReading> readings = readingsByBuilding.computeIfAbsent(reading.buildingId(), id -> new ArrayDeque<>());
        readings.addLast(reading);
        while (readings.size() > maxPerBuilding) {
            readings.removeFirst();
        }
        evictExpired();
    }

    @Override
    public synchronized List<RecentReading> findBetween(UUID buildingId, Instant from, Instant to) {
        evictExpired();
        return readingsByBuilding.getOrDefault(buildingId, new ArrayDeque<>()).stream()
                .filter(reading -> isBetween(reading, from, to))
                .toList();
    }

    @Override
    public synchronized Set<UUID> findBuildingsWithReadingsBetween(Instant from, Instant to) {
        evictExpired();
        Set<UUID> buildings = new LinkedHashSet<>();
        readingsByBuilding.forEach((buildingId, readings) -> {
            if (readings.stream().anyMatch(reading -> isBetween(reading, from, to))) {
                buildings.add(buildingId);
            }
        });
        return buildings;
    }

    private void evictExpired() {
        Instant oldestKept = clock.instant().minus(retention);
        readingsByBuilding.values().forEach(readings ->
                readings.removeIf(reading -> reading.recordedAt().isBefore(oldestKept)));
        readingsByBuilding.values().removeIf(Deque::isEmpty);
    }

    private static boolean isBetween(RecentReading reading, Instant from, Instant to) {
        return !reading.recordedAt().isBefore(from) && !reading.recordedAt().isAfter(to);
    }
}
