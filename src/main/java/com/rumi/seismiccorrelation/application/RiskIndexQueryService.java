package com.rumi.seismiccorrelation.application;

import com.rumi.seismiccorrelation.domain.model.CorrelatedSeismicEvent;
import com.rumi.seismiccorrelation.domain.repository.CorrelatedSeismicEventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RiskIndexQueryService {

    private final CorrelatedSeismicEventRepository correlatedEventRepository;

    public RiskIndexQueryService(CorrelatedSeismicEventRepository correlatedEventRepository) {
        this.correlatedEventRepository = correlatedEventRepository;
    }

    /** US11: the correlation of the building with the most recent risk index. */
    public CorrelatedSeismicEvent getLatestRiskIndex(UUID buildingId) {
        return correlatedEventRepository.findLatestWithRiskIndexByBuildingId(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building " + buildingId + " has no risk index"));
    }
}
