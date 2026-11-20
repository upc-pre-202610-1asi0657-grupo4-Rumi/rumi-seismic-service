package com.rumi.seismiccorrelation.domain.event;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Published when a seismic event is correlated with a building and its risk index is calculated.
 */
public record CorrelatedSeismicEventCreated(
        UUID buildingId,
        UUID riskIndexId,
        UUID seismicEventId,
        String riskLevel,
        Instant calculatedAt
) {
    public CorrelatedSeismicEventCreated {
        Objects.requireNonNull(buildingId, "Building id is required");
        Objects.requireNonNull(riskIndexId, "Risk index id is required");
        Objects.requireNonNull(seismicEventId, "Seismic event id is required");
        Objects.requireNonNull(riskLevel, "Risk level is required");
        Objects.requireNonNull(calculatedAt, "Calculation time is required");
    }

    public static CorrelatedSeismicEventCreated fromCorrelation(CorrelatedSeismicEvent correlation) {
        RiskIndex riskIndex = correlation.getRiskIndex()
                .orElseThrow(() -> new IllegalStateException("Correlation has no calculated risk index"));
        return new CorrelatedSeismicEventCreated(
                correlation.getBuildingId(),
                riskIndex.getId(),
                correlation.getSeismicEventId(),
                riskIndex.getLevel().name(),
                riskIndex.getCalculatedAt()
        );
    }
}
