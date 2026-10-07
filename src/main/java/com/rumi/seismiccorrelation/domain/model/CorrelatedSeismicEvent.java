package com.rumi.seismiccorrelation.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** A seismic event that matched the sensor readings of one building. */
public final class CorrelatedSeismicEvent {

    private final UUID id;
    private final UUID seismicEventId;
    private final UUID buildingId;
    private final Instant correlatedAt;
    private EventStatus status;
    private RiskIndex riskIndex;

    public CorrelatedSeismicEvent(
            UUID id,
            UUID seismicEventId,
            UUID buildingId,
            EventStatus status,
            Instant correlatedAt,
            RiskIndex riskIndex
    ) {
        this.id = Objects.requireNonNull(id, "Correlated event id is required");
        this.seismicEventId = Objects.requireNonNull(seismicEventId, "Seismic event id is required");
        this.buildingId = Objects.requireNonNull(buildingId, "Building id is required");
        this.status = Objects.requireNonNull(status, "Status is required");
        this.correlatedAt = Objects.requireNonNull(correlatedAt, "Correlation time is required");
        this.riskIndex = riskIndex;
    }

    public static CorrelatedSeismicEvent detect(UUID seismicEventId, UUID buildingId, Instant correlatedAt) {
        return new CorrelatedSeismicEvent(
                UUID.randomUUID(), seismicEventId, buildingId, EventStatus.DETECTED, correlatedAt, null);
    }

    /** DETECTED -> ANALYZED once the risk index is calculated. */
    public void analyze(RiskIndex calculatedRiskIndex) {
        if (status != EventStatus.DETECTED) {
            throw new IllegalStateException("Only a DETECTED event can be analyzed, this one is " + status);
        }
        this.riskIndex = Objects.requireNonNull(calculatedRiskIndex, "Risk index is required");
        this.status = EventStatus.ANALYZED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeismicEventId() {
        return seismicEventId;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCorrelatedAt() {
        return correlatedAt;
    }

    public Optional<RiskIndex> getRiskIndex() {
        return Optional.ofNullable(riskIndex);
    }
}
