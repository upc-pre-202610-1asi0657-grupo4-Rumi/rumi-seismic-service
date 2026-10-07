package com.rumi.seismiccorrelation.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class RiskIndex {

    private final UUID id;
    private final RiskLevel level;
    private final Instant calculatedAt;
    private final String modelVersion;
    private final List<AffectedZoneReport> affectedZones;

    public RiskIndex(
            UUID id,
            RiskLevel level,
            Instant calculatedAt,
            String modelVersion,
            List<AffectedZoneReport> affectedZones
    ) {
        this.id = Objects.requireNonNull(id, "Risk index id is required");
        this.level = Objects.requireNonNull(level, "Risk level is required");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "Calculation time is required");
        this.modelVersion = Objects.requireNonNull(modelVersion, "Model version is required");
        if (!level.isAtLeast(RiskLevel.MEDIUM) && !affectedZones.isEmpty()) {
            throw new IllegalArgumentException("Affected zones are only reported from MEDIUM risk upwards");
        }
        this.affectedZones = affectedZones.stream()
                .sorted(Comparator.comparingInt(AffectedZoneReport::severityRank))
                .toList();
    }

    public UUID getId() {
        return id;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    /** Affected zones ordered by severity rank; empty when the level is below MEDIUM. */
    public List<AffectedZoneReport> getAffectedZones() {
        return affectedZones;
    }
}
