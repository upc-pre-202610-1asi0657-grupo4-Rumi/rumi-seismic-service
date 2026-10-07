package com.rumi.seismiccorrelation.infrastructure.web.dto;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Risk index of a building after a seismic event")
public record RiskIndexResponse(
        @Schema(description = "Risk index id", example = "c3a1e8d2-6b7f-4d09-9a21-5e8f7b6c4d3a")
        UUID id,
        @Schema(description = "Building the risk index belongs to", example = "7a9b3c1d-2e4f-4b6a-8c0d-1e2f3a4b5c6d")
        UUID buildingId,
        @Schema(description = "Correlation of the seismic event with the building",
                example = "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4")
        UUID correlatedEventId,
        @Schema(description = "Risk level", example = "HIGH")
        RiskLevel level,
        @Schema(description = "When the risk index was calculated", example = "2026-10-06T15:32:10Z")
        Instant calculatedAt,
        @Schema(description = "Model that calculated the risk index", example = "rule-based-v1")
        String modelVersion
) {
    public static RiskIndexResponse fromDomain(CorrelatedSeismicEvent correlation) {
        RiskIndex riskIndex = correlation.getRiskIndex().orElseThrow();
        return new RiskIndexResponse(
                riskIndex.getId(),
                correlation.getBuildingId(),
                correlation.getId(),
                riskIndex.getLevel(),
                riskIndex.getCalculatedAt(),
                riskIndex.getModelVersion()
        );
    }
}
