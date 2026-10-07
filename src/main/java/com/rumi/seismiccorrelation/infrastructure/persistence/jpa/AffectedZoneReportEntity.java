package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import com.rumi.seismiccorrelation.domain.model.AffectedZoneReport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "affected_zone_reports")
public class AffectedZoneReportEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_index_id", nullable = false)
    private RiskIndexEntity riskIndex;

    @Column(name = "zone", nullable = false, length = 50)
    private String zone;

    @Column(name = "severity_rank", nullable = false)
    private int severityRank;

    protected AffectedZoneReportEntity() {
    }

    static AffectedZoneReportEntity fromDomain(AffectedZoneReport report, RiskIndexEntity riskIndex) {
        AffectedZoneReportEntity entity = new AffectedZoneReportEntity();
        entity.id = report.id();
        entity.riskIndex = riskIndex;
        entity.zone = report.zone();
        entity.severityRank = report.severityRank();
        return entity;
    }

    AffectedZoneReport toDomain() {
        return new AffectedZoneReport(id, zone, severityRank);
    }
}
