package com.rumi.seismiccorrelation.infrastructure.web.dto;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Seismic event reported by the IGP and stored by Rumi")
public record SeismicEventResponse(
        @Schema(description = "Id of the stored event", example = "1b2c3d4e-5f60-4718-8293-a4b5c6d7e8f9")
        UUID id,
        @Schema(description = "When the earthquake occurred", example = "2026-10-06T15:29:41Z")
        Instant occurredAt,
        MagnitudeResponse magnitude,
        EpicenterResponse epicenter,
        @Schema(description = "Agency that reported the event", example = "IGP")
        String source
) {
    @Schema(description = "Magnitude of the event")
    public record MagnitudeResponse(
            @Schema(description = "Magnitude value", example = "5.8")
            double value,
            @Schema(description = "Magnitude scale", example = "Mw")
            String scale
    ) {
    }

    @Schema(description = "Epicenter of the event")
    public record EpicenterResponse(
            @Schema(description = "Latitude in degrees", example = "-12.05", nullable = true)
            Double latitude,
            @Schema(description = "Longitude in degrees", example = "-77.12", nullable = true)
            Double longitude,
            @Schema(description = "Reference of the epicenter", example = "Callao, 35 km SW", nullable = true)
            String description
    ) {
    }

    public static SeismicEventResponse fromDomain(SeismicEvent event) {
        return new SeismicEventResponse(
                UUID.fromString(event.getId()),
                event.getOccurredAt(),
                new MagnitudeResponse(event.getMagnitude(), event.getMagnitudeScale()),
                new EpicenterResponse(
                        finiteOrNull(event.getLatitude()),
                        finiteOrNull(event.getLongitude()),
                        event.getEpicenterDescription()
                ),
                event.getSource()
        );
    }

    private static Double finiteOrNull(double value) {
        return Double.isFinite(value) ? value : null;
    }
}
