package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.model.EventStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "correlated_seismic_events",
        indexes = @Index(name = "ix_correlated_seismic_events_building", columnList = "building_id")
)
public class CorrelatedSeismicEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "seismic_event_id", nullable = false)
    private UUID seismicEventId;

    // Read-only association, only declared so that the schema gets the foreign key.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seismic_event_id", insertable = false, updatable = false)
    private SeismicEventEntity seismicEvent;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "correlated_at", nullable = false)
    private Instant correlatedAt;

    @OneToOne(mappedBy = "correlatedEvent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RiskIndexEntity riskIndex;

    protected CorrelatedSeismicEventEntity() {
    }

    static CorrelatedSeismicEventEntity fromDomain(CorrelatedSeismicEvent correlatedEvent) {
        CorrelatedSeismicEventEntity entity = new CorrelatedSeismicEventEntity();
        entity.id = correlatedEvent.getId();
        entity.seismicEventId = correlatedEvent.getSeismicEventId();
        entity.buildingId = correlatedEvent.getBuildingId();
        entity.status = correlatedEvent.getStatus();
        entity.correlatedAt = correlatedEvent.getCorrelatedAt();
        entity.riskIndex = correlatedEvent.getRiskIndex()
                .map(riskIndex -> RiskIndexEntity.fromDomain(riskIndex, entity))
                .orElse(null);
        return entity;
    }

    CorrelatedSeismicEvent toDomain() {
        return new CorrelatedSeismicEvent(
                id,
                seismicEventId,
                buildingId,
                status,
                correlatedAt,
                riskIndex == null ? null : riskIndex.toDomain()
        );
    }
}
