package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataCorrelatedSeismicEventRepository
        extends JpaRepository<CorrelatedSeismicEventEntity, UUID> {

    boolean existsBySeismicEventIdAndBuildingId(UUID seismicEventId, UUID buildingId);

    @Query("""
            select correlation from CorrelatedSeismicEventEntity correlation
            join correlation.riskIndex riskIndex
            where correlation.buildingId = :buildingId
            order by riskIndex.calculatedAt desc
            """)
    List<CorrelatedSeismicEventEntity> findWithRiskIndexByBuildingIdNewestFirst(
            @Param("buildingId") UUID buildingId,
            Pageable pageable
    );
}
