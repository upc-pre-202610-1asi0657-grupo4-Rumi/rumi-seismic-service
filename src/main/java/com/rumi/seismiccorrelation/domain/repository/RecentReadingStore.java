package com.rumi.seismiccorrelation.domain.repository;

import com.rumi.seismiccorrelation.domain.model.RecentReading;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Short-lived store of the readings received from rumi-monitoring-service. */
public interface RecentReadingStore {

    void add(RecentReading reading);

    /** Readings of the building recorded between from and to, both inclusive. */
    List<RecentReading> findBetween(UUID buildingId, Instant from, Instant to);

    /** Buildings with at least one reading recorded between from and to, both inclusive. */
    Set<UUID> findBuildingsWithReadingsBetween(Instant from, Instant to);
}
