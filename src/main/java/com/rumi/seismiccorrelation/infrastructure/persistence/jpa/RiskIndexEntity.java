package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.RiskIndex;
import com.rumi.seismiccorrelation.domain.model.RiskLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "risk_indexes")
public class RiskIndexEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "correlated_event_id", nullable = false, unique = true)
    private CorrelatedSeismicEventEntity correlatedEvent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "level", nullable = false, length = 10)
    private RiskLevel level;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "model_version", nullable = false, length = 30)
    private String modelVersion;

    @OneToMany(mappedBy = "riskIndex", cascade = CascadeType.ALL)
    @OrderBy("severityRank ASC")
    private List<AffectedZoneReportEntity> affectedZones = new ArrayList<>();

    protected RiskIndexEntity() {
    }

    static RiskIndexEntity fromDomain(RiskIndex riskIndex, CorrelatedSeismicEventEntity correlatedEvent) {
        RiskIndexEntity entity = new RiskIndexEntity();
        entity.id = riskIndex.getId();
        entity.correlatedEvent = correlatedEvent;
        entity.level = riskIndex.getLevel();
        entity.calculatedAt = riskIndex.getCalculatedAt();
        entity.modelVersion = riskIndex.getModelVersion();
        riskIndex.getAffectedZones().forEach(zone ->
                entity.affectedZones.add(AffectedZoneReportEntity.fromDomain(zone, entity)));
        return entity;
    }

    RiskIndex toDomain() {
        return new RiskIndex(
                id,
                level,
                calculatedAt,
                modelVersion,
                affectedZones.stream().map(AffectedZoneReportEntity::toDomain).toList()
        );
    }
}
