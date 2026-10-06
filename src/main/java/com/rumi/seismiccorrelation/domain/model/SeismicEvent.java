package com.rumi.seismiccorrelation.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Seismic event reported by the IGP. Before it is stored, {@code id} is the IGP code of the
 * event; once stored, it is the UUID assigned by this service.
 */
public final class SeismicEvent {

    public static final String DEFAULT_MAGNITUDE_SCALE = "Mw";
    public static final String IGP_SOURCE = "IGP";

    private final String id;
    private final double magnitude;
    private final String magnitudeScale;
    private final Instant occurredAt;
    private final double latitude;
    private final double longitude;
    private final double depth;
    private final String epicenterDescription;

    public SeismicEvent(
            String id,
            double magnitude,
            Instant occurredAt,
            double latitude,
            double longitude,
            double depth
    ) {
        this(id, magnitude, DEFAULT_MAGNITUDE_SCALE, occurredAt, latitude, longitude, depth, null);
    }

    public SeismicEvent(
            String id,
            double magnitude,
            String magnitudeScale,
            Instant occurredAt,
            double latitude,
            double longitude,
            double depth,
            String epicenterDescription
    ) {
        this.id = Objects.requireNonNull(id, "Seismic event id is required");
        this.magnitude = magnitude;
        this.magnitudeScale = magnitudeScale == null || magnitudeScale.isBlank()
                ? DEFAULT_MAGNITUDE_SCALE
                : magnitudeScale;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurrence time is required");
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.epicenterDescription = epicenterDescription;
    }

    /** Same occurrence time, magnitude and epicenter: the IGP reported the same event again. */
    public boolean isSameOccurrenceAs(SeismicEvent other) {
        return occurredAt.equals(other.occurredAt)
                && Double.compare(magnitude, other.magnitude) == 0
                && Double.compare(latitude, other.latitude) == 0
                && Double.compare(longitude, other.longitude) == 0;
    }

    public String getId() {
        return id;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public String getMagnitudeScale() {
        return magnitudeScale;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Depth in km as reported by the IGP. Not stored: NaN for events read from the database. */
    public double getDepth() {
        return depth;
    }

    public String getEpicenterDescription() {
        return epicenterDescription;
    }

    /** Every event comes from the IGP feed for now. */
    public String getSource() {
        return IGP_SOURCE;
    }
}
