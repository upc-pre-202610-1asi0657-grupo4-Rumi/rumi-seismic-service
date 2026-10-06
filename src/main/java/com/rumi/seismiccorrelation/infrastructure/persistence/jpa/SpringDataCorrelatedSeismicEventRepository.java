package com.rumi.seismiccorrelation.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCorrelatedSeismicEventRepository
        extends JpaRepository<CorrelatedSeismicEventEntity, UUID> {

    boolean existsBySeismicEventIdAndBuildingId(UUID seismicEventId, UUID buildingId);
}
