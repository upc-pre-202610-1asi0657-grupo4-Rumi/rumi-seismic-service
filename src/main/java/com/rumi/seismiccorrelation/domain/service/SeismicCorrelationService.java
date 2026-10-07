package com.rumi.seismiccorrelation.domain.service;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.RecentReading;
import com.rumi.seismiccorrelation.domain.model.SeismicEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Decides whether readings belong to a seismic event: a reading matches when it was recorded
 * between the occurrence of the event and the end of the correlation window.
 */
public final class SeismicCorrelationService {

    private final Duration window;
    private final double vibrationReference;

    public SeismicCorrelationService(Duration window, double vibrationReference) {
        this.window = Objects.requireNonNull(window, "Correlation window is required");
        if (window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Correlation window must be positive");
        }
        if (!(vibrationReference > 0)) {
            throw new IllegalArgumentException("Vibration reference must be positive");
        }
        this.vibrationReference = vibrationReference;
    }

    public Instant windowStart(SeismicEvent event) {
        return event.getOccurredAt();
    }

    public Instant windowEnd(SeismicEvent event) {
        return event.getOccurredAt().plus(window);
    }

    public Duration window() {
        return window;
    }

    public CorrelatedSeismicEvent correlate(SeismicEvent event, UUID buildingId, Instant correlatedAt) {
        return CorrelatedSeismicEvent.detect(UUID.fromString(event.getId()), buildingId, correlatedAt);
    }

    /**
     * Structural response of every zone, from 0 to 1: its peak vibration divided by the reference
     * vibration, capped at 1. Ordered by zone.
     */
    public Map<String, Double> normalizedResponseByZone(List<RecentReading> readings) {
        Map<String, Double> peakByZone = new TreeMap<>();
        readings.forEach(reading -> peakByZone.merge(reading.zone(), Math.abs(reading.vibration()), Math::max));
        peakByZone.replaceAll((zone, peak) -> Math.min(peak / vibrationReference, 1.0));
        return peakByZone;
    }
}
