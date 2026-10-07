package com.rumi.seismiccorrelation.domain.model;

import java.util.Objects;
import java.util.UUID;

/** A zone of the building affected by the event; rank 1 is the most affected. */
public record AffectedZoneReport(UUID id, String zone, int severityRank) {

    public AffectedZoneReport {
        Objects.requireNonNull(id, "Affected zone report id is required");
        Objects.requireNonNull(zone, "Zone is required");
        if (severityRank < 1) {
            throw new IllegalArgumentException("Severity rank starts at 1");
        }
    }
}
