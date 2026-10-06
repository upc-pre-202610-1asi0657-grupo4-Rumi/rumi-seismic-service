package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.SeismicEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "seismic_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seismic_events_occurrence",
                columnNames = {"occurred_at", "magnitude_value", "epicenter_latitude", "epicenter_longitude"}
        )
)
public class SeismicEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "magnitude_value", nullable = false)
    private double magnitudeValue;

    @Column(name = "magnitude_scale", nullable = false, length = 10)
    private String magnitudeScale;

    @Column(name = "epicenter_latitude")
    private Double epicenterLatitude;

    @Column(name = "epicenter_longitude")
    private Double epicenterLongitude;

    @Column(name = "epicenter_description", length = 150)
    private String epicenterDescription;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    protected SeismicEventEntity() {
    }

    static SeismicEventEntity fromDomain(UUID id, SeismicEvent event) {
        SeismicEventEntity entity = new SeismicEventEntity();
        entity.id = id;
        entity.occurredAt = event.getOccurredAt();
        entity.magnitudeValue = event.getMagnitude();
        entity.magnitudeScale = event.getMagnitudeScale();
        entity.epicenterLatitude = event.getLatitude();
        entity.epicenterLongitude = event.getLongitude();
        entity.epicenterDescription = event.getEpicenterDescription();
        entity.source = event.getSource();
        return entity;
    }

    SeismicEvent toDomain() {
        return new SeismicEvent(
                id.toString(),
                magnitudeValue,
                magnitudeScale,
                occurredAt,
                epicenterLatitude == null ? Double.NaN : epicenterLatitude,
                epicenterLongitude == null ? Double.NaN : epicenterLongitude,
                Double.NaN,
                epicenterDescription
        );
    }
}
