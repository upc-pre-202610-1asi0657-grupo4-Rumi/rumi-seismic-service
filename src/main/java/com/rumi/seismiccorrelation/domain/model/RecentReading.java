package com.rumi.seismiccorrelation.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Vibration reported by a sensor of a building, kept for a short time to correlate it. */
public record RecentReading(UUID buildingId, String zone, Instant recordedAt, double vibration) {

    public RecentReading {
        Objects.requireNonNull(buildingId, "Building id is required");
        Objects.requireNonNull(zone, "Zone is required");
        Objects.requireNonNull(recordedAt, "Recording time is required");
    }
}
