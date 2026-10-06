package com.rumi.seismiccorrelation.infrastructure.external.igp;

import java.time.Instant;

/**
 * Latest event as delivered by the IGP feed. magnitudeScale and reference (a description of the
 * epicenter, for example "Callao, 35 km SW") are optional.
 */
public record IgpSeismicEventResponse(
        String code,
        double magnitude,
        String magnitudeScale,
        Instant timestamp,
        double latitude,
        double longitude,
        double depthKm,
        String reference
) {
    public IgpSeismicEventResponse(
            String code,
            double magnitude,
            Instant timestamp,
            double latitude,
            double longitude,
            double depthKm
    ) {
        this(code, magnitude, null, timestamp, latitude, longitude, depthKm, null);
    }
}
