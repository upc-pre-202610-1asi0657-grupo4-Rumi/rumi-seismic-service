package com.rumi.seismiccorrelation.domain.repository;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;

import java.util.Optional;
import java.util.UUID;

/** Stores the correlation aggregate together with its risk index and affected zones. */
public interface CorrelatedSeismicEventRepository {

    CorrelatedSeismicEvent save(CorrelatedSeismicEvent correlatedEvent);

    Optional<CorrelatedSeismicEvent> findById(UUID id);

    boolean existsBySeismicEventIdAndBuildingId(UUID seismicEventId, UUID buildingId);

    /** The correlation of the building whose risk index was calculated last. */
    Optional<CorrelatedSeismicEvent> findLatestWithRiskIndexByBuildingId(UUID buildingId);

    /** The correlation that produced the risk index. */
    Optional<CorrelatedSeismicEvent> findByRiskIndexId(UUID riskIndexId);
}
